package com.company.platform.gateway.log;

import java.time.Instant;

public record GatewayLogEntry(
        Instant timestamp,
        String level,
        String category,
        String service,
        String method,
        String path,
        Integer status,
        String userId,
        Long durationMs,
        String message
) {
}
