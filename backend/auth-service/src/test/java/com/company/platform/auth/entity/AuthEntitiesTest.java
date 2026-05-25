package com.company.platform.auth.entity;

import org.testng.annotations.Test;

import java.time.LocalDateTime;

import static org.testng.Assert.*;

class AuthEntitiesTest {

    @Test
    void rolePrePersistCreatesId() {
        Role role = new Role();

        role.prePersist();

        assertNotNull(role.getId());
    }

    @Test
    void userSessionPrePersistCreatesIdAndDefaultsLastActive() {
        UserSession session = new UserSession();
        LocalDateTime createdAt = LocalDateTime.parse("2026-05-24T10:00:00");
        session.setCreatedAt(createdAt);
        session.setExpired(true);

        session.prePersistSession();

        assertNotNull(session.getId());
        assertEquals(session.getLastActive(), createdAt);
        assertFalse(session.isExpired());
    }
}
