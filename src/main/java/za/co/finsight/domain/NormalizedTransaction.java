package za.co.finsight.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Canonical transaction produced by every source adapter. Amounts are signed: negative for money out,
 * positive for money in. All sources must map into this shape before categorisation.
 */
public record NormalizedTransaction(
        SourceSystem source,
        String externalId,
        String customerId,
        Instant occurredAt,
        BigDecimal amount,
        String currency,
        String description,
        String merchantName,
        String mcc) {

    public Direction direction() {
        return amount.signum() < 0 ? Direction.DEBIT : Direction.CREDIT;
    }
}
