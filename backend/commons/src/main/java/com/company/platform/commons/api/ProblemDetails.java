package com.company.platform.commons.api;

import java.time.LocalDateTime;
import java.util.Map;

public record ProblemDetails(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        LocalDateTime timestamp,
        String traceId,
        Map<String, String> validationErrors
) {
}
