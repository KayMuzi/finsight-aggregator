package za.co.finsight.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.co.finsight.analytics.AnalyticsService;
import za.co.finsight.budget.BudgetService;

@RestController
@RequestMapping(value = "/api/v1/customers/{customerId}/budgets", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class BudgetController {

    private final BudgetService budgetService;
    private final AnalyticsService analyticsService;

    @GetMapping
    public List<BudgetService.BudgetView> list(
            @PathVariable @Pattern(regexp = ApiModels.CUSTOMER_ID) String customerId) {
        return budgetService.list(customerId);
    }

    @GetMapping("/{month}")
    public BudgetService.BudgetView get(
            @PathVariable @Pattern(regexp = ApiModels.CUSTOMER_ID) String customerId,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return budgetService.get(customerId, month);
    }

    /** Create or replace the budget for a month. Transactions in that month are re-categorised against it. */
    @PutMapping(value = "/{month}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BudgetService.BudgetView upsert(
            @PathVariable @Pattern(regexp = ApiModels.CUSTOMER_ID) String customerId,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @Valid @RequestBody ApiModels.BudgetRequest body) {
        return budgetService.upsert(customerId, month, body);
    }

    @DeleteMapping("/{month}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable @Pattern(regexp = ApiModels.CUSTOMER_ID) String customerId,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        budgetService.delete(customerId, month);
    }

    @GetMapping("/{month}/health")
    public AnalyticsService.BudgetHealthView health(
            @PathVariable @Pattern(regexp = ApiModels.CUSTOMER_ID) String customerId,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return analyticsService.budgetHealth(customerId, month);
    }
}
