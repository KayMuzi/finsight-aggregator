package za.co.finsight.ingestion;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.finsight.domain.TransactionRecord;
import za.co.finsight.repository.TransactionRepository;

/**
 * Flags cross-source duplicates: the same amount appearing in two different feeds within one hour
 * (for example a card purchase that also shows on the bank statement). The earlier record stays
 * POSTED; the later one is marked DUPLICATE and excluded from aggregates. This is a heuristic and
 * should be tuned against labelled data before production use.
 */
@Component
@RequiredArgsConstructor
public class DuplicateDetector {

    static final Duration TOLERANCE = Duration.ofHours(1);

    private final TransactionRepository transactions;

    public Optional<TransactionRecord> findOriginal(TransactionRecord candidate) {
        return transactions.findPossibleDuplicates(
                        candidate.getCustomerId(),
                        candidate.getSource(),
                        candidate.getAmount(),
                        candidate.getOccurredAt().minus(TOLERANCE),
                        candidate.getOccurredAt().plus(TOLERANCE))
                .stream()
                .findFirst();
    }
}
