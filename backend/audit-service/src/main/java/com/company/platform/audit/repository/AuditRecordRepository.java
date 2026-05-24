package com.company.platform.audit.repository;

import com.company.platform.audit.model.AuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditRecordRepository extends JpaRepository<AuditRecord, UUID> {
    List<AuditRecord> findTop100ByOrderByCreatedAtDesc();

    boolean existsByUserIdAndTraceId(UUID userId, String traceId);
}
