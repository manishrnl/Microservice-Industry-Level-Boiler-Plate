package com.company.platform.gateway.log;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

@Service
public class GatewayLogService {
    private static final int MAX_ENTRIES = 2_000;

    private final LinkedList<GatewayLogEntry> entries = new LinkedList<>();

    public void record(GatewayLogEntry entry) {
        synchronized (entries) {
            entries.addFirst(entry);
            while (entries.size() > MAX_ENTRIES) {
                entries.removeLast();
            }
        }
    }

    public void recordRequest(String method, String path, String userId, Long durationMs, Integer status) {
        record(new GatewayLogEntry(
                Instant.now(),
                levelFor(status),
                "RUNTIME",
                serviceFor(path),
                method,
                path,
                status,
                userId,
                durationMs,
                "gateway_request method=%s path=%s userId=%s durationMs=%s status=%s".formatted(
                        method,
                        path,
                        blank(userId),
                        durationMs,
                        status
                )
        ));
    }

    public void recordSecurity(String path, String detail, Integer status) {
        record(new GatewayLogEntry(
                Instant.now(),
                "WARN",
                "SECURITY",
                serviceFor(path),
                null,
                path,
                status,
                null,
                null,
                "gateway_security path=%s status=%s detail=%s".formatted(path, status, detail)
        ));
    }

    public List<GatewayLogEntry> search(String query, String level, String category, String service, Integer status, int limit) {
        String normalizedQuery = normalize(query);
        String normalizedLevel = normalize(level);
        String normalizedCategory = normalize(category);
        String normalizedService = normalize(service);
        int boundedLimit = Math.max(1, Math.min(limit, 500));
        List<GatewayLogEntry> snapshot;
        synchronized (entries) {
            snapshot = new ArrayList<>(entries);
        }
        return snapshot.stream()
                .filter(entry -> matches(entry, normalizedQuery, normalizedLevel, normalizedCategory, normalizedService, status))
                .sorted(Comparator.comparing(GatewayLogEntry::timestamp).reversed())
                .limit(boundedLimit)
                .toList();
    }

    private boolean matches(GatewayLogEntry entry, String query, String level, String category, String service, Integer status) {
        return (level.isBlank() || level.equalsIgnoreCase(entry.level()))
                && (category.isBlank() || category.equalsIgnoreCase(entry.category()))
                && (service.isBlank() || service.equalsIgnoreCase(entry.service()))
                && (status == null || status.equals(entry.status()))
                && (query.isBlank() || searchable(entry).contains(query));
    }

    private String searchable(GatewayLogEntry entry) {
        return String.join(" ",
                blank(entry.level()),
                blank(entry.category()),
                blank(entry.service()),
                blank(entry.method()),
                blank(entry.path()),
                entry.status() == null ? "" : entry.status().toString(),
                blank(entry.userId()),
                blank(entry.message())
        ).toLowerCase(Locale.ROOT);
    }

    private String serviceFor(String path) {
        if (!StringUtils.hasText(path)) {
            return "gateway";
        }
        if (path.startsWith("/api/v1/auth")) {
            return "auth-service";
        }
        if (path.startsWith("/api/v1/users")) {
            return "user-service";
        }
        if (path.startsWith("/api/v1/notifications")) {
            return "notification-service";
        }
        if (path.startsWith("/api/v1/payments")) {
            return "payment-service";
        }
        if (path.startsWith("/api/v1/files")) {
            return "file-service";
        }
        if (path.startsWith("/api/v1/ai")) {
            return "ai-service";
        }
        if (path.startsWith("/api/v1/audit")) {
            return "audit-service";
        }
        return "gateway";
    }

    private String levelFor(Integer status) {
        if (status != null && status >= 500) {
            return "ERROR";
        }
        if (status != null && status >= 400) {
            return "WARN";
        }
        return "INFO";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String blank(String value) {
        return value == null ? "" : value;
    }
}
