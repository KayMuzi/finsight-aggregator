package za.co.finsight.categorisation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import za.co.finsight.domain.Category;
import za.co.finsight.domain.Direction;

/**
 * A declarative categorisation rule loaded from rules/categorisation-rules.json.
 * Criteria within one rule are AND-ed. Keywords inside one rule are OR-ed.
 * Rules are evaluated in ascending priority; the first match wins.
 */
public record CategoryRule(
        String id,
        int priority,
        Category category,
        Direction direction,
        Set<String> mcc,
        List<String> keywords,
        BigDecimal minAmount,
        BigDecimal maxAmount) {

    public CategoryRule {
        mcc = mcc == null ? Set.of() : Set.copyOf(mcc);
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
    }

    public boolean hasCriteria() {
        return direction != null || !mcc.isEmpty() || !keywords.isEmpty() || minAmount != null || maxAmount != null;
    }
}
