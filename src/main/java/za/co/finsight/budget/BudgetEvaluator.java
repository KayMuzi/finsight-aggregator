package za.co.finsight.budget;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import za.co.finsight.domain.Category;
import za.co.finsight.domain.TransactionBudgetStatus;

/**
 * Pure, side-effect-free evaluation of budget status in chronological order.
 * A transaction is WITHIN_BUDGET if the cumulative net spend in its category, including this
 * transaction, is still at or under the category limit. Refunds reduce cumulative spend.
 */
public final class BudgetEvaluator {

    /** One transaction as seen by the evaluator. spend is positive for money out and negative for refunds. */
    public record Candidate(Long id, Category category, BigDecimal spend, boolean posted) {}

    private BudgetEvaluator() {}

    public static Map<Long, TransactionBudgetStatus> evaluate(List<Candidate> chronological,
                                                              Map<Category, BigDecimal> limits) {
        Map<Category, BigDecimal> running = new EnumMap<>(Category.class);
        Map<Long, TransactionBudgetStatus> result = new LinkedHashMap<>();

        for (Candidate c : chronological) {
            if (!c.posted() || !c.category().budgetable()) {
                result.put(c.id(), TransactionBudgetStatus.NOT_APPLICABLE);
                continue;
            }
            BigDecimal cumulative = running.getOrDefault(c.category(), BigDecimal.ZERO).add(c.spend());
            running.put(c.category(), cumulative);

            BigDecimal limit = limits.get(c.category());
            if (limit == null) {
                result.put(c.id(), TransactionBudgetStatus.NO_BUDGET);
            } else {
                result.put(c.id(), cumulative.compareTo(limit) <= 0
                        ? TransactionBudgetStatus.WITHIN_BUDGET
                        : TransactionBudgetStatus.OVER_BUDGET);
            }
        }
        return result;
    }
}
