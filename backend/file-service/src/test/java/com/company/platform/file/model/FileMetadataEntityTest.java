package com.company.platform.file.model;

import org.testng.annotations.Test;

import java.time.LocalDateTime;

import static org.testng.Assert.*;

class FileMetadataEntityTest {

    @Test
    void prePersistCreatesMissingIdAndTimestamp() {
        FileMetadata metadata = new FileMetadata();

        metadata.prePersist();

        assertNotNull(metadata.getId());
        assertNotNull(metadata.getCreatedAt());
    }

    @Test
    void prePersistKeepsExistingTimestamp() {
        LocalDateTime createdAt = LocalDateTime.parse("2026-05-24T10:00:00");
        FileMetadata metadata = new FileMetadata();
        metadata.setCreatedAt(createdAt);

        metadata.prePersist();

        assertEquals(metadata.getCreatedAt(), createdAt);
    }
}
