package com.company.platform.ai.repository;

import com.company.platform.ai.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    List<ChatMessage> findTop20BySessionIdOrderByCreatedAtDesc(UUID sessionId);

    @Query("""
            select coalesce(sum(message.tokensUsed), 0)
            from ChatMessage message, ChatSession session
            where message.sessionId = session.id
              and session.userId = :userId
              and session.archived = false
            """)
    long sumTokensUsedByUserId(@Param("userId") UUID userId);
}
