package com.company.platform.ai.repository;

import com.company.platform.ai.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    List<ChatSession> findByUserIdAndArchivedFalseOrderByUpdatedAtDesc(UUID userId);

    Optional<ChatSession> findByIdAndUserIdAndArchivedFalse(UUID id, UUID userId);

    boolean existsByUserIdAndTitle(UUID userId, String title);
}
