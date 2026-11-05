package za.co.finsight.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.finsight.analytics.AnalyticsService;

@RestController
@RequestMapping(value = "/api/v1/customers/{customerId}", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class AnalyticsController {

    private final AnalyticsService analytics;

    @GetMapping("/summary/{month}")
    public AnalyticsService.Summary summary(
            @PathVariable @Pattern(regexp = ApiModels.CUSTOMER_ID) String customerId,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return analytics.summary(customerId, month);
    }

    @GetMapping("/analytics/categories/{month}")
    public AnalyticsService.CategoryBreakdown categories(
            @PathVariable @Pattern(regexp = ApiModels.CUSTOMER_ID) String customerId,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return analytics.categoryBreakdown(customerId, month);
    }

    @GetMapping("/analytics/merchants/{month}")
    public List<AnalyticsService.MerchantAmount> merchants(
            @PathVariable @Pattern(regexp = ApiModels.CUSTOMER_ID) String customerId,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return analytics.topMerchants(customerId, month, limit);
    }

    @GetMapping("/analytics/daily/{month}")
    public List<AnalyticsService.DailySpend> daily(
            @PathVariable @Pattern(regexp = ApiModels.CUSTOMER_ID) String customerId,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return analytics.dailySpend(customerId, month);
    }

    @GetMapping("/analytics/trend")
    public List<AnalyticsService.TrendMonth> trend(
            @PathVariable @Pattern(regexp = ApiModels.CUSTOMER_ID) String customerId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth to) {
        return analytics.trend(customerId, from, to);
    }
}
