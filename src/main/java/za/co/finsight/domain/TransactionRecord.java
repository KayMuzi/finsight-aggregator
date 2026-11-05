package za.co.finsight.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SourceSystem source;

    @Column(name = "external_id", nullable = false, length = 128)
    private String externalId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Direction direction;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "merchant_name", nullable = false, length = 128)
    private String merchantName;

    @Column(length = 4)
    private String mcc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_source", nullable = false, length = 16)
    private CategorySource categorySource;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_status", nullable = false, length = 32)
    private TransactionBudgetStatus budgetStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionStatus status;

    @Column(name = "duplicate_of_id")
    private Long duplicateOfId;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public TransactionRecord(NormalizedTransaction n, Category category, Instant now) {
        this.customerId = n.customerId();
        this.source = n.source();
        this.externalId = n.externalId();
        this.occurredAt = n.occurredAt();
        this.amount = n.amount();
        this.currency = n.currency();
        this.direction = n.direction();
        this.description = n.description();
        this.merchantName = n.merchantName();
        this.mcc = n.mcc();
        this.category = category;
        this.categorySource = CategorySource.RULE;
        this.budgetStatus = TransactionBudgetStatus.NOT_APPLICABLE;
        this.status = TransactionStatus.POSTED;
        this.ingestedAt = now;
        this.updatedAt = now;
    }

    public void recategorise(Category newCategory, Instant now) {
        this.category = newCategory;
        this.categorySource = CategorySource.USER;
        this.updatedAt = now;
    }

    public void applyBudgetStatus(TransactionBudgetStatus status) {
        this.budgetStatus = status;
    }

    public void markDuplicateOf(Long originalId) {
        this.status = TransactionStatus.DUPLICATE;
        this.duplicateOfId = originalId;
    }
}
