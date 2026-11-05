package za.co.finsight.transaction;

import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import za.co.finsight.domain.Category;
import za.co.finsight.domain.SourceSystem;
import za.co.finsight.domain.TransactionBudgetStatus;
import za.co.finsight.domain.TransactionRecord;
import za.co.finsight.domain.TransactionStatus;

/** Composable filters for transaction search. Null filter values are ignored. */
public final class TransactionSpecifications {

    public record Filter(Instant from, Instant to, Category category, SourceSystem source,
                         TransactionBudgetStatus budgetStatus, String merchant, boolean includeDuplicates) {}

    private TransactionSpecifications() {}

    public static Specification<TransactionRecord> matching(String customerId, Filter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("customerId"), customerId));
            if (!f.includeDuplicates()) {
                p.add(cb.equal(root.get("status"), TransactionStatus.POSTED));
            }
            if (f.from() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), f.from()));
            }
            if (f.to() != null) {
                p.add(cb.lessThan(root.get("occurredAt"), f.to()));
            }
            if (f.category() != null) {
                p.add(cb.equal(root.get("category"), f.category()));
            }
            if (f.source() != null) {
                p.add(cb.equal(root.get("source"), f.source()));
            }
            if (f.budgetStatus() != null) {
                p.add(cb.equal(root.get("budgetStatus"), f.budgetStatus()));
            }
            if (f.merchant() != null && !f.merchant().isBlank()) {
                p.add(cb.like(cb.upper(root.get("merchantName")),
                        "%" + f.merchant().trim().toUpperCase(Locale.ROOT) + "%"));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}
