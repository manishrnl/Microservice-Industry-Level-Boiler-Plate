package com.company.platform.commons.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class DateUtil {
    private DateUtil() {
    }

    public static String iso(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    public static ZonedDateTime toZone(Instant instant, String zone) {
        return instant.atZone(ZoneId.of(zone));
    }
}
