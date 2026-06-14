package com.company.platform.ai.controller;

import com.company.platform.ai.service.AiChatService;
import com.company.platform.commons.dto.DemoUserRequestDto;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
    private final AiChatService chats;

    public AiController(AiChatService chats) {
        this.chats = chats;
    }

    @PostMapping("/chat")
    Mono<Map<String, Object>> chat(@RequestBody ChatPayload payload,
                                   @RequestHeader("X-User-Id") UUID userId,
                                   @RequestHeader(value = "X-User-Name", required = false) String userName,
                                   @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        return chats.chat(userId, payload, userName, userEmail);
    }

    @GetMapping(value = "/chat/stream/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Map<String, Object>> stream(@PathVariable("sessionId") String sessionId) {
        return Flux.just(Map.of("done", true, "sessionId", sessionId));
    }

    @GetMapping("/sessions")
    List<Map<String, Object>> sessions(@RequestHeader("X-User-Id") UUID userId) {
        return chats.sessions(userId);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    List<Map<String, Object>> messages(@RequestHeader("X-User-Id") UUID userId,
                                       @PathVariable("sessionId") UUID sessionId) {
        return chats.messages(userId, sessionId);
    }

    @PostMapping("/sessions")
    Map<String, Object> createSession(@RequestHeader("X-User-Id") UUID userId,
                                      @RequestBody Map<String, Object> body) {
        return chats.createSession(userId, String.valueOf(body.getOrDefault("title", "New chat")));
    }

    @PatchMapping("/sessions/{sessionId}")
    Map<String, Object> renameSession(@RequestHeader("X-User-Id") UUID userId,
                                      @PathVariable("sessionId") UUID sessionId,
                                      @RequestBody Map<String, Object> body) {
        return chats.renameSession(userId, sessionId, String.valueOf(body.getOrDefault("title", "New chat")));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    Map<String, Object> saveClientMessages(@RequestHeader("X-User-Id") UUID userId,
                                           @PathVariable("sessionId") UUID sessionId,
                                           @RequestBody List<AiChatService.MessagePayload> messages) {
        return chats.saveClientMessages(userId, sessionId, messages);
    }

    @DeleteMapping("/sessions/{sessionId}")
    void archive(@RequestHeader("X-User-Id") UUID userId,
                 @PathVariable("sessionId") UUID sessionId) {
        chats.archive(userId, sessionId);
    }

    @GetMapping("/usage")
    Map<String, Object> usage(@RequestHeader("X-User-Id") UUID userId) {
        return chats.usage(userId);
    }

    @PostMapping("/internal/demo-data")
    List<Map<String, Object>> seedDemoData(@RequestBody DemoUserRequestDto request) {
        return chats.seedDemoData(request);
    }

    @PostMapping("/admin/system-prompt")
    void updatePrompt(@RequestBody Map<String, String> body) {
    }

    public record ChatPayload(String sessionId, String message, Boolean stream, String systemPrompt, LocationPayload location,
                       UserContextPayload context) {
    }

    public record LocationPayload(Double latitude, Double longitude, Double accuracy) {
    }

    public record UserContextPayload(String locale, String timezone, String localTime) {
    }
}
