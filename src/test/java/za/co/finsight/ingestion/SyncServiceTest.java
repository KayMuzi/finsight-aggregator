package za.co.finsight.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import za.co.finsight.budget.BudgetService;
import za.co.finsight.categorisation.CategorisationEngine;
import za.co.finsight.config.FinsightProperties;
import za.co.finsight.domain.NormalizedTransaction;
import za.co.finsight.domain.SourceSystem;
import za.co.finsight.domain.SyncRun;
import za.co.finsight.domain.SyncStatus;
import za.co.finsight.domain.TransactionRecord;
import za.co.finsight.repository.SyncRunRepository;
import za.co.finsight.repository.TransactionRepository;
import za.co.finsight.source.SourceUnavailableException;
import za.co.finsight.source.TransactionSource;

class SyncServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-15T10:00:00Z");

    @Test
    void failingSourceMarksRunPartialAndHealthySourcesAreStillIngested() throws Exception {
        NormalizedTransaction t1 = tx(SourceSystem.BANK, "B-1", "-100.00");
        NormalizedTransaction t2 = tx(SourceSystem.BANK, "B-2", "-50.00");

        TransactionRepository transactions = mock(TransactionRepository.class);
        SyncRunRepository syncRuns = mock(SyncRunRepository.class);
        DuplicateDetector duplicates = mock(DuplicateDetector.class);
        BudgetService budgets = mock(BudgetService.class);

        when(transactions.findByCustomerIdAndSourceAndExternalId(anyString(), any(), anyString()))
                .thenReturn(Optional.empty());
        when(transactions.save(any(TransactionRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(syncRuns.save(any(SyncRun.class))).thenAnswer(inv -> inv.getArgument(0));
        when(duplicates.findOriginal(any())).thenReturn(Optional.empty());

        FinsightProperties props = new FinsightProperties(
                new FinsightProperties.Security("test-api-key-0123456789"),
                "Africa/Johannesburg",
                new FinsightProperties.Budget(80),
                new FinsightProperties.Mock(List.of()));

        SyncService service = new SyncService(
                List.of(stub(SourceSystem.BANK, List.of(t1, t2), null),
                        stub(SourceSystem.CARD, List.of(), "connection refused")),
                transactions, syncRuns, duplicates,
                new CategorisationEngine(new ObjectMapper()), budgets,
                Clock.fixed(NOW, ZoneOffset.UTC), props);

        SyncService.SyncResult result = service.sync("CUST-T");

        assertThat(result.status()).isEqualTo(SyncStatus.PARTIAL);
        assertThat(result.fetched()).isEqualTo(2);
        assertThat(result.inserted()).isEqualTo(2);
        assertThat(result.sources()).anySatisfy(o -> {
            assertThat(o.source()).isEqualTo(SourceSystem.CARD);
            assertThat(o.status()).isEqualTo("FAILED");
        });
        verify(budgets, atLeastOnce()).recalculateMonth(eq("CUST-T"), any(YearMonth.class));
    }

    private static NormalizedTransaction tx(SourceSystem source, String id, String amount) {
        return new NormalizedTransaction(source, id, "CUST-T", NOW, new BigDecimal(amount), "ZAR",
                "POS PURCHASE CHECKERS", "Checkers", null);
    }

    private static TransactionSource stub(SourceSystem system, List<NormalizedTransaction> rows, String error) {
        return new TransactionSource() {
            @Override
            public SourceSystem system() {
                return system;
            }

            @Override
            public List<NormalizedTransaction> fetch(String customerId) {
                if (error != null) {
                    throw new SourceUnavailableException(system, error);
                }
                return rows;
            }
        };
    }
}
