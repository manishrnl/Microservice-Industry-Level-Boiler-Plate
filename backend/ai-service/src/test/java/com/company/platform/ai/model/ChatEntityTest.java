package com.company.platform.ai.model;

import org.testng.annotations.Test;

import java.time.LocalDateTime;

import static org.testng.Assert.*;

class ChatEntityTest {

    @Test
    void chatSessionPrePersistDefaultsIdTitleAndTimestamps() {
        ChatSession session = new ChatSession();

        session.prePersist();

        assertNotNull(session.getId());
        assertEquals(session.getTitle(), "New chat");
        assertNotNull(session.getCreatedAt());
        assertEquals(session.getUpdatedAt(), session.getCreatedAt());
    }

    @Test
    void chatSessionPreUpdateRefreshesUpdatedAt() {
        ChatSession session = new ChatSession();
        session.setTitle("Existing");
        session.prePersist();

        session.preUpdate();

        assertNotNull(session.getUpdatedAt());
    }

    @Test
    void chatMessagePrePersistDefaultsIdAndTimestamp() {
        ChatMessage message = new ChatMessage();
        LocalDateTime createdAt = LocalDateTime.parse("2026-05-24T10:00:00");
        message.setCreatedAt(createdAt);

        message.prePersist();

        assertNotNull(message.getId());
        assertEquals(message.getCreatedAt(), createdAt);
    }
}
