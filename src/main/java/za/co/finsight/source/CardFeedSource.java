package za.co.finsight.source;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import za.co.finsight.domain.NormalizedTransaction;
import za.co.finsight.domain.SourceSystem;

/** Mock card-acquirer feed. Native format: amounts in minor units (cents), merchant name and MCC. */
@Component
public class CardFeedSource implements TransactionSource {

    private static final String FEED = "mock/card-feed.json";

    public record CardRecord(String authorisationId, String customerId, String authorisedAt, long amountMinor,
                             String currency, Merchant merchant) {}

    public record Merchant(String name, String mcc) {}

    private final List<CardRecord> feed;
    private final MockOutages outages;

    public CardFeedSource(ObjectMapper mapper, MockOutages outages) throws IOException {
        this.feed = MockFeeds.load(mapper, FEED, new TypeReference<List<CardRecord>>() {});
        this.outages = outages;
    }

    @Override
    public SourceSystem system() {
        return SourceSystem.CARD;
    }

    @Override
    public List<NormalizedTransaction> fetch(String customerId) {
        outages.assertAvailable(customerId, system());
        return feed.stream()
                .filter(r -> customerId.equals(r.customerId()))
                .map(this::normalise)
                .toList();
    }

    NormalizedTransaction normalise(CardRecord r) {
        return new NormalizedTransaction(
                SourceSystem.CARD,
                r.authorisationId(),
                r.customerId(),
                OffsetDateTime.parse(r.authorisedAt()).toInstant(),
                BigDecimal.valueOf(r.amountMinor(), 2),
                r.currency(),
                "CARD " + r.merchant().name(),
                Merchants.titleCase(r.merchant().name()),
                r.merchant().mcc());
    }
}
