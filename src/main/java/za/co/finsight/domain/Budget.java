package za.co.finsight.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A user's budget for one calendar month (yearMonth in YYYY-MM form). */
@Entity
@Table(name = "budgets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "budget_month", nullable = false, length = 7)
    private String yearMonth;

    @Column(name = "total_limit", precision = 19, scale = 2)
    private BigDecimal totalLimit;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "budget_limits", joinColumns = @JoinColumn(name = "budget_id"))
    private List<BudgetLimit> limits = new ArrayList<>();

    public Budget(String customerId, String yearMonth, Instant now) {
        this.customerId = customerId;
        this.yearMonth = yearMonth;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void replace(BigDecimal totalLimit, List<BudgetLimit> lines, Instant now) {
        this.totalLimit = totalLimit;
        this.limits.clear();
        this.limits.addAll(lines);
        this.updatedAt = now;
    }

    public Map<Category, BigDecimal> limitsByCategory() {
        Map<Category, BigDecimal> map = new EnumMap<>(Category.class);
        limits.forEach(l -> map.put(l.getCategory(), l.getAmount()));
        return map;
    }
}
