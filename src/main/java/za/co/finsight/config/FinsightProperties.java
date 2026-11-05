package za.co.finsight.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import za.co.finsight.domain.SourceSystem;

@Validated
@ConfigurationProperties(prefix = "finsight")
public record FinsightProperties(
        @Valid @NotNull Security security,
        @NotBlank String timezone,
        @Valid @NotNull Budget budget,
        @Valid @NotNull Mock mock) {

    public ZoneId zone() {
        return ZoneId.of(timezone);
    }

    public record Security(@NotBlank @Size(min = 16) String apiKey) {}

    public record Budget(@Min(1) @Max(100) int nearLimitPercent) {}

    public record Mock(@NotNull List<Outage> outages) {
        public record Outage(@NotBlank String customerId, @NotNull SourceSystem source) {}
    }
}
