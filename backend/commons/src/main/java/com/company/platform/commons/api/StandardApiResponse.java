package com.company.platform.commons.api;

import java.time.LocalDateTime;

public record StandardApiResponse<T>(
        boolean success,
        int status,
        String message,
        T data,
        LocalDateTime timestamp,
        String traceId
) {
    public static <T> StandardApiResponse<T> ok(String message, T data, String traceId) {
        return new StandardApiResponse<>(true, 200, message, data, LocalDateTime.now(), traceId);
    }
}
