package com.company.platform.notification.model;

import com.company.platform.commons.enums.NotificationCategory;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.testng.Assert.*;

class NotificationEntityTest {

    @Test
    void prePersistCreatesMissingIdAndCreatedAt() {
        Notification notification = new Notification();
        notification.setUserId(UUID.randomUUID());
        notification.setCategory(NotificationCategory.SYSTEM);
        notification.setTitle("Welcome");

        notification.prePersist();

        assertNotNull(notification.getId());
        assertNotNull(notification.getCreatedAt());
    }

    @Test
    void prePersistKeepsExistingCreatedAt() {
        LocalDateTime createdAt = LocalDateTime.parse("2026-05-24T10:00:00");
        Notification notification = new Notification();
        notification.setCreatedAt(createdAt);

        notification.prePersist();

        assertEquals(notification.getCreatedAt(), createdAt);
    }
}
