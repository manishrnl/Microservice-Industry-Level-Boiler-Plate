package com.company.platform.ai.repository;

import com.company.platform.ai.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    List<ChatSession> findByUserIdAndArchivedFalseOrderByUpdatedAtDesc(UUID userId);

    Optional<ChatSession> findByIdAndUserIdAndArchivedFalse(UUID id, UUID userId);

    boolean existsByUserIdAndTitle(UUID userId, String title);

    @Query("select coalesce(sum(session.totalTokens), 0) from ChatSession session where session.userId = :userId")
    long sumTotalTokensByUserId(@Param("userId") UUID userId);
}
