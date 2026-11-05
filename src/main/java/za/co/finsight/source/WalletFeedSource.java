package za.co.finsight.source;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import za.co.finsight.domain.NormalizedTransaction;
import za.co.finsight.domain.SourceSystem;

/** Mock mobile-wallet feed. Native format: event types, UTC instants, counterparty names. */
@Component
public class   WalletFeedSource implements TransactionSource {

    private static final String FEED = "mock/wallet-feed.json";

    public record WalletRecord(String reference, String customerId, String eventType, String createdAt,
                               String signedAmount, String currency, String counterparty) {}

    private final List<WalletRecord> feed;
    private final MockOutages outages;

    public WalletFeedSource(ObjectMapper mapper, MockOutages outages) throws IOException {
        this.feed = MockFeeds.load(mapper, FEED, new TypeReference<List<WalletRecord>>() {});
        this.outages = outages;
    }

    @Override
    public SourceSystem system() {
        return SourceSystem.WALLET;
    }

    @Override
    public List<NormalizedTransaction> fetch(String customerId) {
        outages.assertAvailable(customerId, system());
        return feed.stream()
                .filter(r -> customerId.equals(r.customerId()))
                .map(this::normalise)
                .toList();
    }

    NormalizedTransaction normalise(WalletRecord r) {
        return new NormalizedTransaction(
                SourceSystem.WALLET,
                r.reference(),
                r.customerId(),
                Instant.parse(r.createdAt()),
                new BigDecimal(r.signedAmount()).setScale(2, RoundingMode.HALF_UP),
                r.currency(),
                r.eventType().replace('_', ' ') + " " + r.counterparty(),
                Merchants.titleCase(r.counterparty()),
                null);
    }
}
