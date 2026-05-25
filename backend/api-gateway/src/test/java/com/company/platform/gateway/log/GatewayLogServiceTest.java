package com.company.platform.gateway.log;

import org.testng.annotations.Test;

import java.time.Instant;

import static org.testng.Assert.*;

class GatewayLogServiceTest {

    @Test
    void recordRequestClassifiesServiceAndLevelFromPathAndStatus() {
        GatewayLogService logs = new GatewayLogService();

        logs.recordRequest("GET", "/api/v1/auth/me", "user-1", 12L, 200);
        logs.recordRequest("GET", "/api/v1/users/me", "user-1", 12L, 404);
        logs.recordRequest("GET", "/api/v1/payments", "user-1", 12L, 503);
        logs.recordSecurity("/api/v1/users/me", "Missing token", 401);

        assertEquals(logs.search(null, "INFO", "RUNTIME", "auth-service", 200, 10).size(), 1);
        assertEquals(logs.search(null, "WARN", "RUNTIME", "user-service", 404, 10).size(), 1);
        assertEquals(logs.search(null, "ERROR", "RUNTIME", "payment-service", 503, 10).size(), 1);
        assertEquals(logs.search("missing", "WARN", "SECURITY", "user-service", 401, 10).size(), 1);
    }

    @Test
    void searchBoundsLimitAndMatchesAcrossSearchableFields() {
        GatewayLogService logs = new GatewayLogService();
        logs.record(new GatewayLogEntry(Instant.parse("2026-05-24T10:00:00Z"), "INFO", "RUNTIME",
                "ai-service", "POST", "/api/v1/ai/chat", 200, "u1", 20L, "gateway_request ai"));
        logs.record(new GatewayLogEntry(Instant.parse("2026-05-24T10:01:00Z"), "INFO", "RUNTIME",
                "audit-service", "GET", "/api/v1/audit", 200, "u2", 30L, "gateway_request audit"));

        assertEquals(logs.search("gateway_request", null, null, null, null, 0).size(), 1);
        assertEquals(
                logs.search("audit", null, null, null, null, 10).stream().map(GatewayLogEntry::service).toList(),
                java.util.List.of("audit-service")
        );
        assertEquals(logs.search(null, null, null, null, null, 9999).size(), 2);
    }
}
