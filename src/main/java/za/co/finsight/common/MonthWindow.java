package za.co.finsight.common;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;

/** Half-open time range [from, to) covering one calendar month in the configured zone. */
public record MonthWindow(Instant from, Instant to) {

    public static MonthWindow of(YearMonth month, ZoneId zone) {
        return new MonthWindow(
                month.atDay(1).atStartOfDay(zone).toInstant(),
                month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant());
    }
}
