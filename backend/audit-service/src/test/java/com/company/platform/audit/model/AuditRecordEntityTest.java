package com.company.platform.audit.model;

import org.testng.annotations.Test;

import java.time.LocalDateTime;

import static org.testng.Assert.*;

class AuditRecordEntityTest {

    @Test
    void prePersistCreatesMissingIdAndTimestamp() {
        AuditRecord record = new AuditRecord();

        record.prePersist();

        assertNotNull(record.getId());
        assertNotNull(record.getCreatedAt());
    }

    @Test
    void prePersistKeepsExistingTimestamp() throws Exception {
        LocalDateTime createdAt = LocalDateTime.parse("2026-05-24T10:00:00");
        AuditRecord record = new AuditRecord();
        java.lang.reflect.Field field = AuditRecord.class.getDeclaredField("createdAt");
        field.setAccessible(true);
        field.set(record, createdAt);

        record.prePersist();

        assertEquals(record.getCreatedAt(), createdAt);
    }
}
