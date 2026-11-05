package za.co.finsight.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.finsight.budget.BudgetService;
import za.co.finsight.common.MonthWindow;
import za.co.finsight.config.FinsightProperties;
import za.co.finsight.domain.Budget;
import za.co.finsight.domain.BudgetHealth;
import za.co.finsight.domain.Category;
import za.co.finsight.domain.SourceSystem;
import za.co.finsight.domain.TransactionRecord;
import za.co.finsight.domain.TransactionStatus;
import za.co.finsight.exception.BusinessRuleException;
import za.co.finsight.repository.TransactionRepository;
import za.co.finsight.repository.TransactionRepository.CategoryTotal;

/**
 * Read-side aggregations. Spend is net: debits minus refunds within each category, so a refund
 * reduces the category it was originally spent in. Income is the sum of INCOME category credits.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final int TOP_N = 5;
    private static final int MAX_TREND_MONTHS = 24;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public record CategoryAmount(Category category, BigDecimal amount, long count, BigDecimal percentOfExpenses) {}
    public record MerchantAmount(String merchant, BigDecimal amount, long count) {}
    public record SourceAmount(SourceSystem source, BigDecimal netAmount, long count) {}

    public record Summary(String yearMonth, String currency, BigDecimal income, BigDecimal expenses,
                          BigDecimal surplus, BigDecimal savingsContributed, BigDecimal savingsRatePercent,
                          long transactionCount, long duplicatesExcluded, List<CategoryAmount> topCategories,
                          List<MerchantAmount> topMerchants, List<SourceAmount> bySource) {}

    public record CategoryBreakdown(String yearMonth, BigDecimal expenses, List<CategoryAmount> categories) {}

    public record CategoryHealth(Category category, BigDecimal limit, BigDecimal spent, BigDecimal remaining,
                                 BigDecimal percentUsed, BudgetHealth health) {}

    public record BudgetHealthView(String yearMonth, BigDecimal totalLimit, BigDecimal totalSpent,
                                   BigDecimal totalRemaining, BudgetHealth totalHealth,
                                   List<CategoryHealth> categories) {}

    public record TrendMonth(String yearMonth, BigDecimal income, BigDecimal expenses,
                             Map<Category, BigDecimal> expensesByCategory) {}

    public record DailySpend(LocalDate date, BigDecimal amount, long count) {}

    private final TransactionRepository transactions;
    private final BudgetService budgets;
    private final FinsightProperties properties;

    @Transactional(readOnly = true)
    public Summary summary(String customerId, YearMonth month) {
        MonthWindow w = window(month);
        MonthTotals t = totals(customerId, w);
        BigDecimal expenses = t.expenses();
        BigDecimal income = t.income();
        BigDecimal savings = t.spend(Category.SAVINGS);

        List<CategoryAmount> topCategories = Category.budgetableCategories().stream()
                .filter(c -> t.spend(c).signum() > 0)
                .map(c -> new CategoryAmount(c, money(t.spend(c)), t.count(c), percent(t.spend(c), expenses)))
                .sorted(Comparator.comparing(CategoryAmount::amount).reversed())
                .limit(TOP_N)
                .toList();

        List<MerchantAmount> topMerchants = topMerchants(customerId, month, TOP_N);

        List<SourceAmount> bySource = transactions.sumBySource(customerId, w.from(), w.to()).stream()
                .map(s -> new SourceAmount(s.getSource(), money(s.getTotal()), s.getCount()))
                .toList();

        long duplicates = transactions.countByCustomerIdAndStatusAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                customerId, TransactionStatus.DUPLICATE, w.from(), w.to());

        return new Summary(month.toString(), "ZAR", money(income), money(expenses),
                money(income.subtract(expenses)), money(savings), percent(savings, income),
                t.transactionCount(), duplicates, topCategories, topMerchants, bySource);
    }

    @Transactional(readOnly = true)
    public CategoryBreakdown categoryBreakdown(String customerId, YearMonth month) {
        MonthTotals t = totals(customerId, window(month));
        BigDecimal expenses = t.expenses();
        List<CategoryAmount> rows = Category.budgetableCategories().stream()
                .filter(c -> t.count(c) > 0)
                .map(c -> new CategoryAmount(c, money(t.spend(c)), t.count(c), percent(t.spend(c), expenses)))
                .sorted(Comparator.comparing(CategoryAmount::amount).reversed())
                .toList();
        return new CategoryBreakdown(month.toString(), money(expenses), rows);
    }

    @Transactional(readOnly = true)
    public BudgetHealthView budgetHealth(String customerId, YearMonth month) {
        MonthTotals t = totals(customerId, window(month));
        Optional<Budget> budget = budgets.find(customerId, month);
        Map<Category, BigDecimal> limits = budget.map(Budget::limitsByCategory).orElse(Map.of());
        BigDecimal totalLimit = budget.map(Budget::getTotalLimit).orElse(null);

        List<CategoryHealth> rows = new ArrayList<>();
        for (Category c : Category.budgetableCategories()) {
            BigDecimal spent = t.spend(c);
            BigDecimal limit = limits.get(c);
            if (limit == null && spent.signum() == 0) {
                continue;
            }
            rows.add(categoryHealth(limit, spent, c));
        }

        BigDecimal expenses = t.expenses();
        BudgetHealth totalHealth = totalLimit == null ? BudgetHealth.NO_LIMIT : healthFor(expenses, totalLimit);
        return new BudgetHealthView(month.toString(), money(totalLimit), money(expenses),
                totalLimit == null ? null : money(totalLimit.subtract(expenses)), totalHealth, rows);
    }

    @Transactional(readOnly = true)
    public List<TrendMonth> trend(String customerId, YearMonth from, YearMonth to) {
        if (from.isAfter(to)) {
            throw new BusinessRuleException("'from' must not be after 'to'");
        }
        if (ChronoUnit.MONTHS.between(from, to) + 1 > MAX_TREND_MONTHS) {
            throw new BusinessRuleException("Trend range may not exceed " + MAX_TREND_MONTHS + " months");
        }
        List<TrendMonth> out = new ArrayList<>();
        for (YearMonth m = from; !m.isAfter(to); m = m.plusMonths(1)) {
            MonthTotals t = totals(customerId, window(m));
            Map<Category, BigDecimal> byCategory = new EnumMap<>(Category.class);
            Category.budgetableCategories().forEach(c -> byCategory.put(c, money(t.spend(c))));
            out.add(new TrendMonth(m.toString(), money(t.income()), money(t.expenses()), byCategory));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<MerchantAmount> topMerchants(String customerId, YearMonth month, int limit) {
        return transactions.topMerchants(customerId, Category.budgetableCategories(),
                        window(month).from(), window(month).to(), PageRequest.of(0, limit)).stream()
                .filter(m -> m.getTotal().signum() < 0)
                .map(m -> new MerchantAmount(m.getMerchant(), money(m.getTotal().negate()), m.getCount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DailySpend> dailySpend(String customerId, YearMonth month) {
        ZoneId zone = properties.zone();
        MonthWindow w = window(month);
        Map<LocalDate, BigDecimal> amounts = new TreeMap<>();
        Map<LocalDate, Long> counts = new TreeMap<>();
        for (TransactionRecord t : transactions.findInWindow(customerId, w.from(), w.to())) {
            if (t.getStatus() != TransactionStatus.POSTED || !t.getCategory().budgetable()) {
                continue;
            }
            LocalDate day = t.getOccurredAt().atZone(zone).toLocalDate();
            amounts.merge(day, t.getAmount().negate(), BigDecimal::add);
            counts.merge(day, 1L, Long::sum);
        }
        return amounts.entrySet().stream()
                .map(e -> new DailySpend(e.getKey(), money(e.getValue()), counts.get(e.getKey())))
                .toList();
    }

    private CategoryHealth categoryHealth(BigDecimal limit, BigDecimal spent, Category category) {
        if (limit == null) {
            return new CategoryHealth(category, null, money(spent), null, null, BudgetHealth.NO_LIMIT);
        }
        return new CategoryHealth(category, money(limit), money(spent), money(limit.subtract(spent)),
                percent(spent, limit), healthFor(spent, limit));
    }

    private BudgetHealth healthFor(BigDecimal spent, BigDecimal limit) {
        BigDecimal used = spent.multiply(HUNDRED).divide(limit, 4, RoundingMode.HALF_UP);
        if (used.compareTo(HUNDRED) > 0) {
            return BudgetHealth.OVER_LIMIT;
        }
        if (used.compareTo(BigDecimal.valueOf(properties.budget().nearLimitPercent())) >= 0) {
            return BudgetHealth.NEAR_LIMIT;
        }
        return BudgetHealth.ON_TRACK;
    }

    private MonthTotals totals(String customerId, MonthWindow w) {
        Map<Category, BigDecimal> raw = new EnumMap<>(Category.class);
        Map<Category, Long> counts = new EnumMap<>(Category.class);
        for (CategoryTotal row : transactions.sumByCategory(customerId, w.from(), w.to())) {
            raw.put(row.getCategory(), row.getTotal());
            counts.put(row.getCategory(), row.getCount());
        }
        return new MonthTotals(raw, counts);
    }

    private MonthWindow window(YearMonth month) {
        return MonthWindow.of(month, properties.zone());
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        if (part == null || whole == null || whole.signum() == 0) {
            return null;
        }
        return part.multiply(HUNDRED).divide(whole, 1, RoundingMode.HALF_UP);
    }

    /** Raw signed sums per category for one month, with helpers for the derived figures. */
    private record MonthTotals(Map<Category, BigDecimal> raw, Map<Category, Long> counts) {

        BigDecimal income() {
            return raw.getOrDefault(Category.INCOME, BigDecimal.ZERO);
        }

        BigDecimal spend(Category category) {
            return raw.getOrDefault(category, BigDecimal.ZERO).negate();
        }

        BigDecimal expenses() {
            return Category.budgetableCategories().stream()
                    .map(this::spend)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        long count(Category category) {
            return counts.getOrDefault(category, 0L);
        }

        long transactionCount() {
            return counts.values().stream().mapToLong(Long::longValue).sum();
        }
    }
}
