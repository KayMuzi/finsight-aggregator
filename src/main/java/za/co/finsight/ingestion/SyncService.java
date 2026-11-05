package za.co.finsight.ingestion;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.finsight.budget.BudgetService;
import za.co.finsight.categorisation.CategorisationEngine;
import za.co.finsight.config.FinsightProperties;
import za.co.finsight.domain.Category;
import za.co.finsight.domain.NormalizedTransaction;
import za.co.finsight.domain.SourceSystem;
import za.co.finsight.domain.SyncRun;
import za.co.finsight.domain.SyncStatus;
import za.co.finsight.domain.TransactionRecord;
import za.co.finsight.repository.SyncRunRepository;
import za.co.finsight.repository.TransactionRepository;
import za.co.finsight.source.SourceUnavailableException;
import za.co.finsight.source.TransactionSource;

/**
 * Pulls from every configured source, de-duplicates, categorises, and recalculates budgets for each
 * affected month. One failing source is recorded and the run completes as PARTIAL, never aborts.
 */
@Slf4j
@Service
public class SyncService {

    public record SourceOutcome(SourceSystem source, String status, int fetched, String error) {}

    public record SyncResult(Long syncRunId, String customerId, SyncStatus status, Instant startedAt,
                             Instant completedAt, int fetched, int inserted, int duplicates, int alreadyKnown,
                             List<SourceOutcome> sources) {}

    public record SyncRunView(Long id, SyncStatus status, Instant startedAt, Instant completedAt, int fetched,
                              int inserted, int duplicates, int alreadyKnown, String errorSummary) {
        static SyncRunView from(SyncRun r) {
            return new SyncRunView(r.getId(), r.getStatus(), r.getStartedAt(), r.getCompletedAt(),
                    r.getFetchedCount(), r.getInsertedCount(), r.getDuplicateCount(),
                    r.getAlreadyKnownCount(), r.getErrorSummary());
        }
    }

    private final List<TransactionSource> sources;
    private final TransactionRepository transactions;
    private final SyncRunRepository syncRuns;
    private final DuplicateDetector duplicateDetector;
    private final CategorisationEngine categorisation;
    private final BudgetService budgets;
    private final Clock clock;
    private final ZoneId zone;

    public SyncService(List<TransactionSource> sources, TransactionRepository transactions,
                       SyncRunRepository syncRuns, DuplicateDetector duplicateDetector,
                       CategorisationEngine categorisation, BudgetService budgets,
                       Clock clock, FinsightProperties properties) {
        this.sources = sources;
        this.transactions = transactions;
        this.syncRuns = syncRuns;
        this.duplicateDetector = duplicateDetector;
        this.categorisation = categorisation;
        this.budgets = budgets;
        this.clock = clock;
        this.zone = properties.zone();
    }

    @Transactional
    public SyncResult sync(String customerId) {
        Instant startedAt = clock.instant();
        List<SourceOutcome> outcomes = new ArrayList<>();
        List<NormalizedTransaction> fetched = new ArrayList<>();

        for (TransactionSource source : sources) {
            try {
                List<NormalizedTransaction> batch = source.fetch(customerId);
                fetched.addAll(batch);
                outcomes.add(new SourceOutcome(source.system(), "OK", batch.size(), null));
            } catch (SourceUnavailableException ex) {
                log.warn("Source {} unavailable for customer {}: {}", source.system(), customerId, ex.getMessage());
                outcomes.add(new SourceOutcome(source.system(), "FAILED", 0, ex.getMessage()));
            }
        }
        fetched.sort(Comparator.comparing(NormalizedTransaction::occurredAt)
                .thenComparing(NormalizedTransaction::externalId));

        int inserted = 0;
        int duplicates = 0;
        int alreadyKnown = 0;
        Set<YearMonth> touchedMonths = new TreeSet<>();

        for (NormalizedTransaction n : fetched) {
            touchedMonths.add(YearMonth.from(n.occurredAt().atZone(zone)));

            if (transactions.findByCustomerIdAndSourceAndExternalId(customerId, n.source(), n.externalId())
                    .isPresent()) {
                alreadyKnown++;
                continue;
            }

            Category category = categorisation.categorise(n);
            TransactionRecord record = new TransactionRecord(n, category, clock.instant());

            Optional<TransactionRecord> original = duplicateDetector.findOriginal(record);
            original.ifPresent(o -> record.markDuplicateOf(o.getId()));
            if (original.isPresent()) {
                duplicates++;
            }

            transactions.save(record);
            inserted++;
        }

        touchedMonths.forEach(month -> budgets.recalculateMonth(customerId, month));

        SyncStatus status = outcomes.stream().anyMatch(o -> "FAILED".equals(o.status()))
                ? SyncStatus.PARTIAL
                : SyncStatus.SUCCESS;
        String errorSummary = outcomes.stream()
                .filter(o -> "FAILED".equals(o.status()))
                .map(o -> o.source() + ": " + o.error())
                .collect(Collectors.joining("; "));
        Instant completedAt = clock.instant();

        SyncRun run = new SyncRun(customerId, startedAt);
        run.complete(status, fetched.size(), inserted, duplicates, alreadyKnown,
                errorSummary.isEmpty() ? null : errorSummary, completedAt);
        syncRuns.save(run);

        log.info("Sync for {} finished: status={} fetched={} inserted={} duplicates={} alreadyKnown={}",
                customerId, status, fetched.size(), inserted, duplicates, alreadyKnown);

        return new SyncResult(run.getId(), customerId, status, startedAt, completedAt,
                fetched.size(), inserted, duplicates, alreadyKnown, outcomes);
    }

    @Transactional(readOnly = true)
    public List<SyncRunView> history(String customerId) {
        return syncRuns.findTop20ByCustomerIdOrderByStartedAtDesc(customerId).stream()
                .map(SyncRunView::from)
                .toList();
    }
}
