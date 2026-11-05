package za.co.finsight.budget;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import za.co.finsight.domain.Category;
import za.co.finsight.domain.TransactionBudgetStatus;

class BudgetEvaluatorTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void cumulativeSpendDecidesWithinOrOverBudget() {
        List<BudgetEvaluator.Candidate> txns = List.of(
                new BudgetEvaluator.Candidate(1L, Category.GROCERIES, bd("600"), true),
                new BudgetEvaluator.Candidate(2L, Category.GROCERIES, bd("500"), true),
                new BudgetEvaluator.Candidate(3L, Category.GROCERIES, bd("-200"), true));

        Map<Long, TransactionBudgetStatus> result = BudgetEvaluator.evaluate(txns, Map.of(Category.GROCERIES, bd("1000")));

        assertThat(result.get(1L)).isEqualTo(TransactionBudgetStatus.WITHIN_BUDGET);
        assertThat(result.get(2L)).isEqualTo(TransactionBudgetStatus.OVER_BUDGET);
        assertThat(result.get(3L)).isEqualTo(TransactionBudgetStatus.WITHIN_BUDGET);
    }

    @Test
    void categoryWithoutLimitIsNoBudget() {
        var result = BudgetEvaluator.evaluate(
                List.of(new BudgetEvaluator.Candidate(1L, Category.DINING, bd("100"), true)), Map.of());

        assertThat(result.get(1L)).isEqualTo(TransactionBudgetStatus.NO_BUDGET);
    }

    @Test
    void incomeDuplicatesAndUnbudgetableCategoriesAreNotApplicable() {
        var result = BudgetEvaluator.evaluate(List.of(
                new BudgetEvaluator.Candidate(1L, Category.INCOME, bd("-32000"), true),
                new BudgetEvaluator.Candidate(2L, Category.GROCERIES, bd("50"), false),
                new BudgetEvaluator.Candidate(3L, Category.SAVINGS, bd("2000"), true)),
                Map.of(Category.GROCERIES, bd("1000")));

        assertThat(result.values()).containsOnly(TransactionBudgetStatus.NOT_APPLICABLE);
    }

    @Test
    void categoriesAreTrackedIndependently() {
        var result = BudgetEvaluator.evaluate(List.of(
                new BudgetEvaluator.Candidate(1L, Category.GROCERIES, bd("900"), true),
                new BudgetEvaluator.Candidate(2L, Category.DINING, bd("900"), true)),
                Map.of(Category.GROCERIES, bd("1000"), Category.DINING, bd("500")));

        assertThat(result.get(1L)).isEqualTo(TransactionBudgetStatus.WITHIN_BUDGET);
        assertThat(result.get(2L)).isEqualTo(TransactionBudgetStatus.OVER_BUDGET);
    }
}
