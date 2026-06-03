package com.company.platform.audit.service;

import com.company.platform.audit.model.AuditRecord;
import com.company.platform.audit.repository.AuditRecordRepository;
import com.company.platform.commons.dto.AuditEventDto;
import com.company.platform.commons.dto.DemoUserRequestDto;
import com.company.platform.commons.enums.AuditAction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditRecordService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final AuditRecordRepository repository;
    private final ObjectMapper objectMapper;

    public AuditRecordService(AuditRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Cacheable(cacheNames = "auditQuery", key = "'top100'")
    @Transactional(readOnly = true)
    public List<AuditEventDto> query() {
        return repository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    @Cacheable(cacheNames = "auditExport", key = "'all'")
    @Transactional(readOnly = true)
    public List<AuditEventDto> export() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::toDto)
                .toList();
    }

    @CacheEvict(cacheNames = {"auditQuery", "auditExport"}, allEntries = true)
    @Transactional
    public List<AuditEventDto> seedDemoData(DemoUserRequestDto request) {
        UUID userId = request.userId();
        String name = displayName(request);
        createIfMissing(userId, name, AuditAction.CREATE, "user", userId.toString(), "demo-user-created",
                Map.of("account", Map.of("exists", false)),
                Map.of("description", name + " account was created and seeded with demo records."));
        createIfMissing(userId, name, AuditAction.LOGIN, "auth-session", "demo-login-session", "demo-login-success",
                Map.of("session", Map.of("active", false)),
                Map.of("description", name + " signed in successfully with the seeded super-admin account."));
        createIfMissing(userId, name, AuditAction.ROLE_CHANGE, "user-role", userId.toString(), "demo-role-assignment",
                Map.of("roles", List.of("USER")),
                Map.of("roles", List.of("USER", "ADMIN", "SUPER_ADMIN"),
                        "description", name + " received USER, ADMIN, and SUPER_ADMIN roles for the demo environment."));
        createIfMissing(userId, name, AuditAction.READ, "observability", "prometheus", "demo-prometheus-scrape",
                Map.of("target", "auth-service", "scraped", false),
                Map.of("target", "auth-service", "scraped", true,
                        "description", "Prometheus scraped actuator metrics for the demo stack."));
        createIfMissing(userId, name, AuditAction.READ, "observability", "grafana", "demo-grafana-dashboard",
                Map.of("dashboard", "platform-overview", "opened", false),
                Map.of("dashboard", "platform-overview", "opened", true,
                        "description", "Grafana dashboard activity was added so observability screens are not empty."));
        return query();
    }

    private void createIfMissing(UUID userId,
                                 String username,
                                 AuditAction action,
                                 String resourceType,
                                 String resourceId,
                                 String traceId,
                                 Map<String, Object> beforeState,
                                 Map<String, Object> afterState) {
        if (repository.existsByUserIdAndTraceId(userId, traceId)) {
            return;
        }
        AuditRecord record = new AuditRecord();
        record.setUserId(userId);
        record.setUsername(username);
        record.setAction(action);
        record.setResourceType(resourceType);
        record.setResourceId(resourceId);
        record.setIpAddress("127.0.0.1");
        record.setUserAgent("demo-seeder");
        record.setTraceId(traceId);
        record.setBeforeState(writeJson(beforeState));
        record.setAfterState(writeJson(afterState));
        record.setStatus("SUCCESS");
        repository.save(record);
    }

    private AuditEventDto toDto(AuditRecord record) {
        return new AuditEventDto(
                record.getUserId(),
                record.getUsername(),
                record.getAction(),
                record.getResourceType(),
                record.getResourceId(),
                record.getIpAddress(),
                record.getUserAgent(),
                record.getTraceId(),
                readJson(record.getBeforeState()),
                readJson(record.getAfterState()),
                record.getStatus(),
                record.getErrorMessage(),
                record.getCreatedAt()
        );
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private Map<String, Object> readJson(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String displayName(DemoUserRequestDto request) {
        if (request.name() != null && !request.name().isBlank() && !request.name().equalsIgnoreCase(request.email())) {
            return request.name();
        }
        String email = request.email();
        int atIndex = email == null ? -1 : email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : "User";
    }
}
