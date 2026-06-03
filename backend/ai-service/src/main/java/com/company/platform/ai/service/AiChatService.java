package com.company.platform.ai.service;

import com.company.platform.ai.controller.AiController;
import com.company.platform.ai.model.ChatMessage;
import com.company.platform.ai.model.ChatSession;
import com.company.platform.ai.repository.ChatMessageRepository;
import com.company.platform.ai.repository.ChatSessionRepository;
import com.company.platform.commons.dto.DemoUserRequestDto;
import com.company.platform.ai.provider.AiProviderFactory;
import com.company.platform.ai.provider.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiChatService {
    private static final String MODEL_NAME = "gemini-or-groq";

    private final AiProviderFactory providers;
    private final ChatSessionRepository sessions;
    private final ChatMessageRepository messages;

    @Value("${ai.context.max-messages:20}")
    private int maxHistoryMessages;

    @Cacheable(cacheNames = "aiSessions", key = "#userId")
    public List<Map<String, Object>> sessions(UUID userId) {
        return sessions.findByUserIdAndArchivedFalseOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::toSessionMap)
                .toList();
    }

    @Cacheable(cacheNames = "aiMessages", key = "#userId + '|' + #sessionId")
    public List<Map<String, Object>> messages(UUID userId, UUID sessionId) {
        requireSession(userId, sessionId);
        return messages.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(this::toMessageMap)
                .toList();
    }

    @CacheEvict(cacheNames = {"aiSessions", "aiMessages"}, allEntries = true, beforeInvocation = true)
    public Map<String, Object> createSession(UUID userId, String title) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(cleanTitle(title));
        return toSessionMap(sessions.save(session));
    }

    @CacheEvict(cacheNames = {"aiSessions", "aiMessages"}, allEntries = true, beforeInvocation = true)
    public Map<String, Object> renameSession(UUID userId, UUID sessionId, String title) {
        ChatSession session = requireSession(userId, sessionId);
        session.setTitle(cleanTitle(title));
        return toSessionMap(sessions.save(session));
    }

    @CacheEvict(cacheNames = {"aiSessions", "aiMessages"}, allEntries = true, beforeInvocation = true)
    public void archive(UUID userId, UUID sessionId) {
        ChatSession session = requireSession(userId, sessionId);
        session.setArchived(true);
        sessions.save(session);
    }

    @CacheEvict(cacheNames = {"aiSessions", "aiMessages"}, allEntries = true, beforeInvocation = true)
    public Map<String, Object> saveClientMessages(UUID userId, UUID sessionId, List<MessagePayload> payloads) {
        ChatSession session = requireSession(userId, sessionId);
        for (MessagePayload payload : payloads) {
            if (payload == null || payload.content() == null || payload.content().isBlank()) {
                continue;
            }
            saveMessage(session.getId(), payload.role(), payload.content(), estimateTokens(payload.content()), "client");
        }
        touchSession(session, payloads.stream()
                .filter(payload -> payload != null && payload.role() != null && payload.role().equalsIgnoreCase("user"))
                .map(MessagePayload::content)
                .filter(content -> content != null && !content.isBlank())
                .findFirst()
                .orElse(null), 0);
        return toSessionMap(session);
    }

    @CacheEvict(cacheNames = {"aiSessions", "aiMessages"}, allEntries = true, beforeInvocation = true)
    public List<Map<String, Object>> seedDemoData(DemoUserRequestDto request) {
        UUID userId = request.userId();
        createDemoChat(userId,
                "Explain this microservice project",
                "Explain this microservice project in simple words.",
                "This platform has a gateway, auth, user, file, payment, AI, notification, and audit services. Each service owns a focused database table set.");
        createDemoChat(userId,
                "Check Prometheus metrics",
                "How can I confirm Prometheus metrics are working?",
                "Open /actuator/prometheus on a service or the Prometheus UI. Grafana can visualize those scraped metrics.");
        createDemoChat(userId,
                "Review Grafana dashboard",
                "What should I look for in Grafana?",
                "Start with service health, request rate, latency, error counts, JVM memory, and database connection pool metrics.");
        createDemoChat(userId,
                "Summarize audit activity",
                "Summarize my latest audit events.",
                "Your sample audit activity includes login, role assignment, session review, Prometheus scrape, and Grafana dashboard access.");
        createDemoChat(userId,
                "Plan a payment test",
                "How do I test the demo payment flow?",
                "Use the payments screen to create or confirm a demo checkout, then review the saved payment history.");
        return sessions(userId);
    }

    @CacheEvict(cacheNames = {"aiSessions", "aiMessages"}, allEntries = true, beforeInvocation = true)
    public Mono<Map<String, Object>> chat(UUID userId, AiController.ChatPayload payload, String userName, String userEmail) {
        ChatSession session = sessionForChat(userId, payload);
        String userContent = payload.message() == null ? "" : payload.message().trim();
        if (userContent.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is required"));
        }
        List<ChatRequest.ChatTurn> history = history(session.getId());
        saveMessage(session.getId(), "user", userContent, estimateTokens(userContent), null);
        String prompt = systemPrompt(payload, userName, userEmail);
        session.setSystemPrompt(prompt);
        session.setContextSummary(contextSummary(payload));
        sessions.save(session);
        long started = System.currentTimeMillis();
        return providers.chat(new ChatRequest(session.getId().toString(), userContent, prompt, history))
                .map(response -> {
                    int outputTokens = estimateTokens(response);
                    saveMessage(session.getId(), "assistant", response, outputTokens, MODEL_NAME);
                    touchSession(session, userContent, outputTokens);
                    return Map.<String, Object>of(
                            "sessionId", session.getId(),
                            "response", response,
                            "tokensUsed", outputTokens,
                            "model", MODEL_NAME,
                            "latencyMs", System.currentTimeMillis() - started
                    );
                })
                .onErrorResume(ex -> {
                    String response = "AI service could not answer right now. Please check the local AI service/provider configuration.";
                    int outputTokens = estimateTokens(response);
                    saveMessage(session.getId(), "assistant", response, outputTokens, MODEL_NAME);
                    touchSession(session, userContent, outputTokens);
                    return Mono.just(Map.<String, Object>of(
                            "sessionId", session.getId(),
                            "response", response,
                            "tokensUsed", outputTokens,
                            "model", MODEL_NAME,
                            "error", true,
                            "latencyMs", System.currentTimeMillis() - started
                    ));
                });
    }

    private ChatSession sessionForChat(UUID userId, AiController.ChatPayload payload) {
        if (payload.sessionId() == null || payload.sessionId().isBlank()) {
            ChatSession session = new ChatSession();
            session.setUserId(userId);
            session.setTitle("New chat");
            return sessions.save(session);
        }
        return requireSession(userId, UUID.fromString(payload.sessionId()));
    }

    private ChatSession requireSession(UUID userId, UUID sessionId) {
        return sessions.findByIdAndUserIdAndArchivedFalse(sessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat session not found"));
    }

    private List<ChatRequest.ChatTurn> history(UUID sessionId) {
        List<ChatMessage> rows = new ArrayList<>(messages.findTop20BySessionIdOrderByCreatedAtDesc(sessionId));
        rows.sort(Comparator.comparing(ChatMessage::getCreatedAt));
        int keep = Math.max(0, maxHistoryMessages);
        return rows.stream()
                .skip(Math.max(0, rows.size() - keep))
                .map(message -> new ChatRequest.ChatTurn(message.getRole(), message.getContent()))
                .toList();
    }

    private ChatMessage saveMessage(UUID sessionId, String role, String content, Integer tokens, String model) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(cleanRole(role));
        message.setContent(content);
        message.setTokensUsed(tokens);
        message.setModel(model);
        return messages.save(message);
    }

    private void createDemoChat(UUID userId, String title, String userMessage, String assistantMessage) {
        String cleanTitle = cleanTitle(title);
        if (sessions.existsByUserIdAndTitle(userId, cleanTitle)) {
            return;
        }
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(cleanTitle);
        session.setModelUsed(MODEL_NAME);
        session.setTotalTokens(estimateTokens(userMessage) + estimateTokens(assistantMessage));
        ChatSession saved = sessions.save(session);
        saveMessage(saved.getId(), "user", userMessage, estimateTokens(userMessage), "demo-user");
        saveMessage(saved.getId(), "assistant", assistantMessage, estimateTokens(assistantMessage), MODEL_NAME);
    }

    private void touchSession(ChatSession session, String firstUserMessage, int outputTokens) {
        if ((session.getTitle() == null || session.getTitle().equals("New chat")) && firstUserMessage != null && !firstUserMessage.isBlank()) {
            session.setTitle(cleanTitle(firstUserMessage));
        }
        session.setModelUsed(MODEL_NAME);
        session.setTotalTokens(session.getTotalTokens() + Math.max(0, outputTokens));
        session.setUpdatedAt(LocalDateTime.now());
        sessions.save(session);
    }

    private String cleanTitle(String title) {
        String value = title == null ? "" : title.trim().replaceAll("\\s+", " ");
        if (value.isBlank()) {
            return "New chat";
        }
        return value.length() > 80 ? value.substring(0, 80) : value;
    }

    private String cleanRole(String role) {
        String value = role == null ? "assistant" : role.toLowerCase();
        return value.equals("user") ? "user" : "assistant";
    }

    private int estimateTokens(String text) {
        return text == null ? 0 : Math.max(1, text.length() / 4);
    }

    private Map<String, Object> toSessionMap(ChatSession session) {
        return Map.of(
                "id", session.getId(),
                "title", session.getTitle(),
                "modelUsed", session.getModelUsed() == null ? "" : session.getModelUsed(),
                "totalTokens", session.getTotalTokens(),
                "createdAt", session.getCreatedAt(),
                "updatedAt", session.getUpdatedAt()
        );
    }

    private Map<String, Object> toMessageMap(ChatMessage message) {
        return Map.of(
                "id", message.getId(),
                "sessionId", message.getSessionId(),
                "role", message.getRole(),
                "content", message.getContent(),
                "tokensUsed", message.getTokensUsed() == null ? 0 : message.getTokensUsed(),
                "model", message.getModel() == null ? "" : message.getModel(),
                "createdAt", message.getCreatedAt()
        );
    }

    private String systemPrompt(AiController.ChatPayload payload, String userName, String userEmail) {
        if (payload.systemPrompt() != null && !payload.systemPrompt().isBlank()) {
            return payload.systemPrompt();
        }
        StringBuilder prompt = new StringBuilder("""
                You are a helpful assistant for this logged-in user. If asked who the user is, use the supplied profile context. If the user asks for "near me", "nearby", or local weather, use the supplied browser location when it is present. If location is not present, say that location permission is needed and ask for a city or permission.
                """);
        if (userName != null && !userName.isBlank()) {
            prompt.append("\nLogged-in user name: ").append(userName);
        }
        if (userEmail != null && !userEmail.isBlank()) {
            prompt.append("\nLogged-in user email: ").append(userEmail);
        }
        if (payload.location() != null) {
            AiController.LocationPayload location = payload.location();
            prompt.append("\nBrowser location: latitude ").append(location.latitude())
                    .append(", longitude ").append(location.longitude());
            if (location.accuracy() != null) {
                prompt.append(", accuracy about ").append(Math.round(location.accuracy())).append(" meters");
            }
        }
        if (payload.context() != null) {
            AiController.UserContextPayload context = payload.context();
            if (context.locale() != null && !context.locale().isBlank()) {
                prompt.append("\nBrowser locale: ").append(context.locale());
            }
            if (context.timezone() != null && !context.timezone().isBlank()) {
                prompt.append("\nBrowser timezone: ").append(context.timezone());
            }
            if (context.localTime() != null && !context.localTime().isBlank()) {
                prompt.append("\nBrowser local time: ").append(context.localTime());
            }
        }
        return prompt.toString();
    }

    private String contextSummary(AiController.ChatPayload payload) {
        if (payload.context() == null) {
            return null;
        }
        AiController.UserContextPayload context = payload.context();
        return "locale=%s timezone=%s".formatted(context.locale(), context.timezone());
    }

    public record MessagePayload(String role, String content) {
    }
}
