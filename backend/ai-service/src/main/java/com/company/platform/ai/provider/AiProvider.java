package com.company.platform.ai.provider;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AiProvider {
    Mono<String> chat(ChatRequest request);

    Flux<String> chatStream(ChatRequest request);

    String getProviderName();
}
