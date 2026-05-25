package com.company.platform.ai.service;

import com.company.platform.ai.controller.AiController;
import com.company.platform.ai.model.ChatMessage;
import com.company.platform.ai.model.ChatSession;
import com.company.platform.ai.provider.AiProviderFactory;
import com.company.platform.ai.provider.ChatRequest;
import com.company.platform.ai.repository.ChatMessageRepository;
import com.company.platform.ai.repository.ChatSessionRepository;
import com.company.platform.commons.dto.DemoUserRequestDto;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

class AiChatServiceTest {
    @Mock
    private AiProviderFactory providers;
    @Mock
    private ChatSessionRepository sessions;
    @Mock
    private ChatMessageRepository messages;

    private AiChatService service;

    @BeforeMethod
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AiChatService(providers, sessions, messages);
        ReflectionTestUtils.setField(service, "maxHistoryMessages", 2);
        given(sessions.save(any(ChatSession.class))).willAnswer(invocation -> {
            ChatSession session = invocation.getArgument(0);
            if (session.getId() == null) {
                session.setId(UUID.randomUUID());
            }
            if (session.getTitle() == null || session.getTitle().isBlank()) {
                session.setTitle("New chat");
            }
            if (session.getCreatedAt() == null) {
                session.setCreatedAt(LocalDateTime.now());
            }
            if (session.getUpdatedAt() == null) {
                session.setUpdatedAt(session.getCreatedAt());
            }
            return session;
        });
        given(messages.save(any(ChatMessage.class))).willAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            if (message.getId() == null) {
                message.setId(UUID.randomUUID());
            }
            if (message.getCreatedAt() == null) {
                message.setCreatedAt(LocalDateTime.now());
            }
            return message;
        });
    }

    @Test
    void sessionsAndMessagesReturnMappedRows() {
        UUID userId = UUID.randomUUID();
        ChatSession session = session(userId, "Support");
        ChatMessage message = message(session.getId(), "user", "Hello", null, null);
        given(sessions.findByUserIdAndArchivedFalseOrderByUpdatedAtDesc(userId)).willReturn(List.of(session));
        given(sessions.findByIdAndUserIdAndArchivedFalse(session.getId(), userId)).willReturn(Optional.of(session));
        given(messages.findBySessionIdOrderByCreatedAtAsc(session.getId())).willReturn(List.of(message));

        List<Map<String, Object>> sessionRows = service.sessions(userId);
        assertEquals(sessionRows.size(), 1);
        assertEquals(sessionRows.getFirst().get("title"), "Support");

        List<Map<String, Object>> messageRows = service.messages(userId, session.getId());
        assertEquals(messageRows.size(), 1);
        assertEquals(messageRows.getFirst().get("role"), "user");
        assertEquals(messageRows.getFirst().get("tokensUsed"), 0);
        assertEquals(messageRows.getFirst().get("model"), "");
    }

    @Test
    void createRenameAndArchiveSessionsUseCleanTitlesAndOwnershipChecks() {
        UUID userId = UUID.randomUUID();
        ChatSession existing = session(userId, "Old");
        given(sessions.findByIdAndUserIdAndArchivedFalse(existing.getId(), userId)).willReturn(Optional.of(existing));

        assertEquals(service.createSession(userId, "  A   long title  ").get("title"), "A long title");
        assertEquals(service.renameSession(userId, existing.getId(), "").get("title"), "New chat");
        service.archive(userId, existing.getId());

        assertTrue(existing.isArchived());
    }

    @Test
    void missingSessionThrowsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        given(sessions.findByIdAndUserIdAndArchivedFalse(sessionId, userId)).willReturn(Optional.empty());

        ResponseStatusException exception = expectThrows(
                ResponseStatusException.class,
                () -> service.messages(userId, sessionId)
        );
        assertEquals(exception.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    @Test
    void saveClientMessagesSkipsBlankPayloadsAndNormalizesAssistantRoles() {
        UUID userId = UUID.randomUUID();
        ChatSession session = session(userId, "New chat");
        given(sessions.findByIdAndUserIdAndArchivedFalse(session.getId(), userId)).willReturn(Optional.of(session));

        service.saveClientMessages(userId, session.getId(), List.of(
                new AiChatService.MessagePayload("user", "First user message"),
                new AiChatService.MessagePayload("system", "Client assistant note"),
                new AiChatService.MessagePayload("user", " ")
        ));

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messages, org.mockito.Mockito.times(2)).save(captor.capture());
        assertEquals(captor.getAllValues().stream().map(ChatMessage::getRole).toList(), List.of("user", "assistant"));
        assertEquals(session.getTitle(), "First user message");
    }

    @Test
    void seedDemoDataCreatesOnlyMissingDemoChats() {
        UUID userId = UUID.randomUUID();
        given(sessions.existsByUserIdAndTitle(userId, "Explain this microservice project")).willReturn(true);
        given(sessions.findByUserIdAndArchivedFalseOrderByUpdatedAtDesc(userId)).willReturn(List.of());

        assertTrue(service.seedDemoData(new DemoUserRequestDto(userId, "u@example.com", "User", "user", null)).isEmpty());

        verify(sessions, org.mockito.Mockito.times(4)).save(any(ChatSession.class));
        verify(messages, org.mockito.Mockito.times(8)).save(any(ChatMessage.class));
    }

    @Test
    void chatRejectsBlankMessage() {
        UUID userId = UUID.randomUUID();
        AiController.ChatPayload payload = new AiController.ChatPayload(null, " ", false, null, null, null);

        StepVerifier.create(service.chat(userId, payload, "MANISH", "u@example.com"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof ResponseStatusException);
                    assertEquals(((ResponseStatusException) error).getStatusCode(), HttpStatus.BAD_REQUEST);
                })
                .verify();
    }

    @Test
    void chatSavesUserAndAssistantMessagesWithContextPrompt() {
        UUID userId = UUID.randomUUID();
        ChatSession session = session(userId, "New chat");
        AiController.ChatPayload payload = new AiController.ChatPayload(session.getId().toString(), "Where am I?",
                false, null, new AiController.LocationPayload(10.0, 20.0, 30.0),
                new AiController.UserContextPayload("en-IN", "Asia/Kolkata", "2026-05-24 18:00"));
        given(sessions.findByIdAndUserIdAndArchivedFalse(session.getId(), userId)).willReturn(Optional.of(session));
        given(messages.findTop20BySessionIdOrderByCreatedAtDesc(session.getId())).willReturn(List.of(
                message(session.getId(), "assistant", "Old answer", 3, "model"),
                message(session.getId(), "user", "Old question", 3, null)
        ));
        given(providers.chat(any(ChatRequest.class))).willReturn(Mono.just("You are near the saved coordinates."));

        StepVerifier.create(service.chat(userId, payload, "MANISH", "u@example.com"))
                .assertNext(body -> {
                    assertEquals(body.get("response"), "You are near the saved coordinates.");
                    assertEquals(body.get("model"), "gemini-or-groq");
                    assertTrue(body.get("latencyMs") instanceof Long);
                })
                .verifyComplete();

        ArgumentCaptor<ChatRequest> requestCaptor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(providers).chat(requestCaptor.capture());
        assertTrue(String.valueOf(requestCaptor.getValue().systemPrompt()).contains(String.valueOf("MANISH")));
        assertTrue(String.valueOf(requestCaptor.getValue().systemPrompt()).contains(String.valueOf("u@example.com")));
        assertTrue(String.valueOf(requestCaptor.getValue().systemPrompt()).contains(String.valueOf("latitude 10.0")));
        assertTrue(String.valueOf(requestCaptor.getValue().systemPrompt()).contains(String.valueOf("Asia/Kolkata")));
        verify(messages, org.mockito.Mockito.times(2)).save(any(ChatMessage.class));
        assertTrue(String.valueOf(session.getSystemPrompt()).contains(String.valueOf("Logged-in user name")));
        assertEquals(session.getContextSummary(), "locale=en-IN timezone=Asia/Kolkata");
    }

    @Test
    void chatStoresFallbackAnswerWhenProviderFails() {
        UUID userId = UUID.randomUUID();
        ChatSession session = session(userId, "New chat");
        AiController.ChatPayload payload = new AiController.ChatPayload(session.getId().toString(), "Hello", false, "Custom prompt", null, null);
        given(sessions.findByIdAndUserIdAndArchivedFalse(session.getId(), userId)).willReturn(Optional.of(session));
        given(messages.findTop20BySessionIdOrderByCreatedAtDesc(session.getId())).willReturn(List.of());
        given(providers.chat(any(ChatRequest.class))).willReturn(Mono.error(new IllegalStateException("provider down")));

        StepVerifier.create(service.chat(userId, payload, null, null))
                .assertNext(body -> {
                    assertEquals(body.get("error"), true);
                    assertTrue(String.valueOf(body.get("response").toString()).contains(String.valueOf("AI service could not answer")));
                })
                .verifyComplete();
    }

    private ChatSession session(UUID userId, String title) {
        ChatSession session = new ChatSession();
        session.setId(UUID.randomUUID());
        session.setUserId(userId);
        session.setTitle(title);
        session.setModelUsed("");
        session.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        session.setUpdatedAt(LocalDateTime.now());
        return session;
    }

    private ChatMessage message(UUID sessionId, String role, String content, Integer tokens, String model) {
        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID());
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setTokensUsed(tokens);
        message.setModel(model);
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }
}
