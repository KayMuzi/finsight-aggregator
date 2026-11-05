package za.co.finsight.budget;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.finsight.api.ApiModels;
import za.co.finsight.common.MonthWindow;
import za.co.finsight.config.FinsightProperties;
import za.co.finsight.domain.Budget;
import za.co.finsight.domain.BudgetLimit;
import za.co.finsight.domain.Category;
import za.co.finsight.domain.TransactionBudgetStatus;
import za.co.finsight.domain.TransactionRecord;
import za.co.finsight.domain.TransactionStatus;
import za.co.finsight.exception.BusinessRuleException;
import za.co.finsight.exception.NotFoundException;
import za.co.finsight.repository.BudgetRepository;
import za.co.finsight.repository.TransactionRepository;

@Service
@RequiredArgsConstructor
public class BudgetService {

    public record LimitView(Category category, BigDecimal amount) {}

    public record BudgetView(String yearMonth, BigDecimal totalLimit, List<LimitView> categoryLimits, Instant updatedAt) {
        static BudgetView from(Budget b) {
            return new BudgetView(b.getYearMonth(), b.getTotalLimit(),
                    b.getLimits().stream().map(l -> new LimitView(l.getCategory(), l.getAmount())).toList(),
                    b.getUpdatedAt());
        }
    }

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactions;
    private final FinsightProperties properties;
    private final Clock clock;

    @Transactional
    public BudgetView upsert(String customerId, YearMonth month, ApiModels.BudgetRequest request) {
        Set<Category> seen = EnumSet.noneOf(Category.class);
        List<BudgetLimit> lines = new ArrayList<>();
        for (ApiModels.CategoryLimit line : request.categoryLimits()) {
            if (!line.category().budgetable()) {
                throw new BusinessRuleException("Category " + line.category() + " cannot be budgeted");
            }
            if (!seen.add(line.category())) {
                throw new BusinessRuleException("Duplicate limit for category " + line.category());
            }
            lines.add(new BudgetLimit(line.category(), money(line.amount())));
        }

        Instant now = clock.instant();
        Budget budget = budgetRepository.findByCustomerIdAndYearMonth(customerId, month.toString())
                .orElseGet(() -> new Budget(customerId, month.toString(), now));
        budget.replace(money(request.totalLimit()), lines, now);
        budgetRepository.save(budget);

        recalculateMonth(customerId, month);
        return BudgetView.from(budget);
    }

    @Transactional(readOnly = true)
    public BudgetView get(String customerId, YearMonth month) {
        return find(customerId, month).map(BudgetView::from)
                .orElseThrow(() -> new NotFoundException("No budget set for " + month));
    }

    @Transactional(readOnly = true)
    public List<BudgetView> list(String customerId) {
        return budgetRepository.findByCustomerIdOrderByYearMonthDesc(customerId).stream()
                .map(BudgetView::from)
                .toList();
    }

    @Transactional
    public void delete(String customerId, YearMonth month) {
        Budget budget = find(customerId, month)
                .orElseThrow(() -> new NotFoundException("No budget set for " + month));
        budgetRepository.delete(budget);
        recalculateMonth(customerId, month);
    }

    @Transactional(readOnly = true)
    public Optional<Budget> find(String customerId, YearMonth month) {
        return budgetRepository.findByCustomerIdAndYearMonth(customerId, month.toString());
    }

    /**
     * Re-derives the budget status of every transaction in the month. Called whenever transactions
     * or budgets change, so that the stored statuses always reflect the current state.
     */
    @Transactional
    public void recalculateMonth(String customerId, YearMonth month) {
        MonthWindow window = MonthWindow.of(month, properties.zone());
        List<TransactionRecord> txns = transactions.findInWindow(customerId, window.from(), window.to());
        Map<Category, BigDecimal> limits = find(customerId, month)
                .map(Budget::limitsByCategory)
                .orElse(Map.of());

        List<BudgetEvaluator.Candidate> candidates = txns.stream()
                .map(t -> new BudgetEvaluator.Candidate(
                        t.getId(), t.getCategory(), t.getAmount().negate(),
                        t.getStatus() == TransactionStatus.POSTED))
                .toList();

        Map<Long, TransactionBudgetStatus> statuses = BudgetEvaluator.evaluate(candidates, limits);
        txns.forEach(t -> t.applyBudgetStatus(statuses.get(t.getId())));
    }

    static BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
