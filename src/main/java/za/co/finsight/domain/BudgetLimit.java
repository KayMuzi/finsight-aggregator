package za.co.finsight.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BudgetLimit {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Category category;

    @Column(name = "limit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    public BudgetLimit(Category category, BigDecimal amount) {
        this.category = category;
        this.amount = amount;
    }
}
