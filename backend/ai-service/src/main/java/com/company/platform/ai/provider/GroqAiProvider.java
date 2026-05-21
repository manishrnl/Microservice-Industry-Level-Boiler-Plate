package com.company.platform.ai.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class GroqAiProvider implements AiProvider {
    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    public GroqAiProvider(@Value("${ai.groq.base-url}") String baseUrl,
                          @Value("${ai.groq.api-key}") String apiKey,
                          @Value("${ai.groq.model}") String model) {
        HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(20));
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public Mono<String> chat(ChatRequest request) {
        Map<String, Object> body = Map.of("model", model, "messages", List.of(Map.of("role", "system", "content", request.systemPrompt() == null ? "You are a helpful assistant." : request.systemPrompt()), Map.of("role", "user", "content", request.message())));
        return webClient.post()
                .uri("/chat/completions")
                .headers(headers -> headers.setBearerAuth(apiKey))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(25))
                .map(this::extractContent);
    }

    @Override
    public Flux<String> chatStream(ChatRequest request) {
        return chat(request).flatMapMany(text -> Flux.fromArray(text.split(" ")).map(token -> token + " "));
    }

    @Override
    public String getProviderName() {
        return "groq";
    }

    private String extractContent(Map<?, ?> response) {
        Object choices = response.get("choices");
        if (choices instanceof List<?> choiceList && !choiceList.isEmpty()
                && choiceList.getFirst() instanceof Map<?, ?> choice
                && choice.get("message") instanceof Map<?, ?> message
                && message.get("content") instanceof String content) {
            return content;
        }
        return response.toString();
    }
}
