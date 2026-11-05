package za.co.finsight.source;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import za.co.finsight.domain.NormalizedTransaction;
import za.co.finsight.domain.SourceSystem;

/** Mock core-banking feed. Native format: signed decimal strings, ISO-8601 offset timestamps, bank narratives. */
@Component
public class BankFeedSource implements TransactionSource {

    private static final String FEED = "mock/bank-feed.json";

    public record BankRecord(String transactionId, String accountNumber, String postingDate, String amount,
                             String currency, String narrative, String customerId) {}

    private final List<BankRecord> feed;
    private final MockOutages outages;

    public BankFeedSource(ObjectMapper mapper, MockOutages outages) throws IOException {
        this.feed = MockFeeds.load(mapper, FEED, new TypeReference<List<BankRecord>>() {});
        this.outages = outages;
    }

    @Override
    public SourceSystem system() {
        return SourceSystem.BANK;
    }

    @Override
    public List<NormalizedTransaction> fetch(String customerId) {
        outages.assertAvailable(customerId, system());
        return feed.stream()
                .filter(r -> customerId.equals(r.customerId()))
                .map(this::normalise)
                .toList();
    }

    NormalizedTransaction normalise(BankRecord r) {
        return new NormalizedTransaction(
                SourceSystem.BANK,
                r.transactionId(),
                r.customerId(),
                OffsetDateTime.parse(r.postingDate()).toInstant(),
                new BigDecimal(r.amount()).setScale(2, RoundingMode.HALF_UP),
                r.currency(),
                r.narrative(),
                Merchants.fromNarrative(r.narrative()),
                null);
    }
}
