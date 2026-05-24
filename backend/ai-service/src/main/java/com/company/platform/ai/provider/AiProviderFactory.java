package com.company.platform.ai.provider;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class AiProviderFactory {
    private static final Duration PROVIDER_TIMEOUT = Duration.ofSeconds(10);

    private final GeminiAiProvider gemini;
    private final GroqAiProvider groq;

    public AiProviderFactory(GeminiAiProvider gemini, GroqAiProvider groq) {
        this.gemini = gemini;
        this.groq = groq;
    }

    public Mono<String> chat(ChatRequest request) {
        return gemini.chat(request)
                .timeout(PROVIDER_TIMEOUT)
                .onErrorResume(ex -> groq.chat(request).timeout(PROVIDER_TIMEOUT));
    }

    public Flux<String> chatStream(ChatRequest request) {
        return gemini.chatStream(request)
                .timeout(PROVIDER_TIMEOUT)
                .onErrorResume(ex -> groq.chatStream(request).timeout(PROVIDER_TIMEOUT));
    }
}
