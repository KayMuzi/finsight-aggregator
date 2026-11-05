package za.co.finsight.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import za.co.finsight.domain.Category;

/** Inbound request bodies and shared API constants. */
public final class ApiModels {

    public static final String CUSTOMER_ID = "[A-Za-z0-9_-]{1,64}";

    private ApiModels() {}

    public record BudgetRequest(
            @Positive @Digits(integer = 13, fraction = 2) BigDecimal totalLimit,
            @NotEmpty List<@Valid CategoryLimit> categoryLimits) {}

    public record CategoryLimit(
            @NotNull Category category,
            @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal amount) {}

    public record CategoryUpdate(@NotNull Category category) {}
}
