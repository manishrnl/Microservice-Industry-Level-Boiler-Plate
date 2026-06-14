package com.company.platform.ai.controller;

import com.company.platform.ai.service.AiChatService;
import com.company.platform.commons.dto.DemoUserRequestDto;
import org.springframework.boot.test.context.SpringBootTest;
import org.testng.annotations.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;

@SpringBootTest
class AiControllerTest {
    private final AiChatService service = mock(AiChatService.class);
    private final AiController controller = new AiController(service);

    @Test
    void delegatesChatSessionAndMessageEndpoints() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        AiController.ChatPayload payload = new AiController.ChatPayload(null, "Hi", false, null, null, null);
        given(service.chat(userId, payload, "MANISH", "u@example.com")).willReturn(Mono.just(Map.of("response", "Hello")));
        given(service.sessions(userId)).willReturn(List.of(Map.of("title", "Chat")));
        given(service.messages(userId, sessionId)).willReturn(List.of(Map.of("role", "user")));
        given(service.createSession(userId, "Work")).willReturn(Map.of("title", "Work"));
        given(service.renameSession(userId, sessionId, "New")).willReturn(Map.of("title", "New"));
        List<AiChatService.MessagePayload> messages = List.of(new AiChatService.MessagePayload("user", "Hi"));
        given(service.saveClientMessages(userId, sessionId, messages)).willReturn(Map.of("id", sessionId));

        StepVerifier.create(controller.chat(payload, userId, "MANISH", "u@example.com"))
                .expectNext(Map.of("response", "Hello"))
                .verifyComplete();
        assertEquals(controller.sessions(userId).size(), 1);
        assertEquals(controller.messages(userId, sessionId).size(), 1);
        assertEquals(controller.createSession(userId, Map.of("title", "Work")).get("title"), "Work");
        assertEquals(controller.renameSession(userId, sessionId, Map.of("title", "New")).get("title"), "New");
        assertEquals(controller.saveClientMessages(userId, sessionId, messages).get("id"), sessionId);
        controller.archive(userId, sessionId);

        verify(service).archive(userId, sessionId);
    }

    @Test
    void simpleUtilityEndpointsReturnStaticOrDelegatedValues() {
        UUID userId = UUID.randomUUID();
        DemoUserRequestDto demo = new DemoUserRequestDto(userId, "u@example.com", "User");
        given(service.seedDemoData(demo)).willReturn(List.of(Map.of("title", "Demo")));
        given(service.usage(userId)).willReturn(Map.of("usedTokens", 100L, "totalTokens", 10000L, "availableTokens", 9900L, "remainingTokens", 9900L, "freeTrialPercent", 10));

        StepVerifier.create(controller.stream("session-1"))
                .expectNext(Map.of("done", true, "sessionId", "session-1"))
                .verifyComplete();
        assertEquals(controller.usage(userId).get("remainingTokens"), 9900L);
        assertEquals(controller.seedDemoData(demo).size(), 1);
        controller.updatePrompt(Map.of("systemPrompt", "Be helpful"));
    }
}
