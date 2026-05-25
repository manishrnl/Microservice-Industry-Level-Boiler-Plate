package com.company.platform.commons.entity;

import org.testng.annotations.Test;

import java.util.UUID;

import static org.testng.Assert.*;

class BaseEntityTest {

    @Test
    void prePersistCreatesIdAndTimestamps() {
        TestEntity entity = new TestEntity();

        entity.prePersist();

        assertNotNull(entity.getId());
        assertNotNull(entity.getCreatedAt());
        assertEquals(entity.getUpdatedAt(), entity.getCreatedAt());
    }

    @Test
    void preUpdateRefreshesUpdatedAtAndKeepsAuditUsers() {
        AuditedEntity entity = new AuditedEntity();
        UUID createdBy = UUID.randomUUID();
        UUID updatedBy = UUID.randomUUID();

        entity.setCreatedBy(createdBy);
        entity.setUpdatedBy(updatedBy);
        entity.prePersist();
        entity.preUpdate();

        assertEquals(entity.getCreatedBy(), createdBy);
        assertEquals(entity.getUpdatedBy(), updatedBy);
        assertNotNull(entity.getUpdatedAt());
    }

    private static final class TestEntity extends BaseEntity {
    }

    private static final class AuditedEntity extends AuditableEntity {
    }
}
