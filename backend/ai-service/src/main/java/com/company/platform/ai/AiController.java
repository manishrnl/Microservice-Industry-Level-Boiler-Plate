package com.company.platform.ai;

import com.company.platform.ai.provider.AiProviderFactory;
import com.company.platform.ai.provider.ChatRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
class AiController {
    private final AiProviderFactory providers;

    AiController(AiProviderFactory providers) {
        this.providers = providers;
    }

    @PostMapping("/chat")
    Mono<Map<String, Object>> chat(@RequestBody ChatPayload payload,
                                   @RequestHeader(value = "X-User-Name", required = false) String userName,
                                   @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        String sessionId = payload.sessionId() == null ? UUID.randomUUID().toString() : payload.sessionId();
        return providers.chat(new ChatRequest(sessionId, payload.message(), systemPrompt(payload, userName, userEmail), List.of()))
                .map(response -> Map.of("sessionId", sessionId, "response", response, "tokensUsed", response.length() / 4, "model", "gemini-or-groq"));
    }

    @GetMapping(value = "/chat/stream/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Map<String, Object>> stream(@PathVariable("sessionId") String sessionId) {
        return providers.chatStream(new ChatRequest(sessionId, "continue", null, List.of()))
                .map(token -> Map.<String, Object>of("token", token, "done", false))
                .concatWithValues(Map.of("done", true, "totalTokens", 0));
    }

    @GetMapping("/sessions")
    List<Map<String, Object>> sessions() {
        return List.of(Map.of("id", UUID.randomUUID(), "title", "New chat", "createdAt", LocalDateTime.now()));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    List<Map<String, Object>> messages(@PathVariable("sessionId") UUID sessionId) {
        return List.of();
    }

    @PostMapping("/sessions")
    Map<String, Object> createSession(@RequestBody Map<String, Object> body) {
        return Map.of("id", UUID.randomUUID());
    }

    @DeleteMapping("/sessions/{sessionId}")
    void archive(@PathVariable("sessionId") UUID sessionId) {
    }

    @GetMapping("/usage")
    Map<String, Object> usage() {
        return Map.of("totalTokens", 0);
    }

    @PostMapping("/admin/system-prompt")
    void updatePrompt(@RequestBody Map<String, String> body) {
    }

    private String systemPrompt(ChatPayload payload, String userName, String userEmail) {
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
            LocationPayload location = payload.location();
            prompt.append("\nBrowser location: latitude ").append(location.latitude())
                    .append(", longitude ").append(location.longitude());
            if (location.accuracy() != null) {
                prompt.append(", accuracy about ").append(Math.round(location.accuracy())).append(" meters");
            }
        }
        if (payload.context() != null) {
            UserContextPayload context = payload.context();
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

    record ChatPayload(String sessionId, String message, Boolean stream, String systemPrompt, LocationPayload location,
                       UserContextPayload context) {
    }

    record LocationPayload(Double latitude, Double longitude, Double accuracy) {
    }

    record UserContextPayload(String locale, String timezone, String localTime) {
    }
}
