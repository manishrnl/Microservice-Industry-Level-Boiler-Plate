package com.company.platform.ai.provider;

import org.testng.annotations.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.BDDMockito.given;

class AiProviderFactoryTest {

    @Test
    void chatUsesGeminiFirstAndFallsBackToGroqOnError() {
        GeminiAiProvider gemini = mock(GeminiAiProvider.class);
        GroqAiProvider groq = mock(GroqAiProvider.class);
        ChatRequest request = new ChatRequest("s1", "hello", "system", List.of());
        given(gemini.chat(request)).willReturn(Mono.error(new IllegalStateException("gemini down")));
        given(groq.chat(request)).willReturn(Mono.just("groq answer"));

        StepVerifier.create(new AiProviderFactory(gemini, groq).chat(request))
                .expectNext("groq answer")
                .verifyComplete();
    }

    @Test
    void chatStreamUsesFallbackProviderWhenPrimaryFails() {
        GeminiAiProvider gemini = mock(GeminiAiProvider.class);
        GroqAiProvider groq = mock(GroqAiProvider.class);
        ChatRequest request = new ChatRequest("s1", "hello", "system", List.of());
        given(gemini.chatStream(request)).willReturn(Flux.error(new IllegalStateException("gemini down")));
        given(groq.chatStream(request)).willReturn(Flux.just("groq ", "answer"));

        StepVerifier.create(new AiProviderFactory(gemini, groq).chatStream(request))
                .expectNext("groq ", "answer")
                .verifyComplete();
    }
}
