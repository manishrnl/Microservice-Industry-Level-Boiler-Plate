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
public class GeminiAiProvider implements AiProvider {
    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    public GeminiAiProvider(@Value("${ai.gemini.base-url}") String baseUrl,
                            @Value("${ai.gemini.api-key}") String apiKey,
                            @Value("${ai.gemini.model}") String model) {
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
        Map<String, Object> body = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", request.systemPrompt() == null ? "You are a helpful assistant." : request.systemPrompt()))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", sanitize(request.message()))))),
                "generationConfig", Map.of("temperature", 0.7, "maxOutputTokens", 2048, "topP", 0.95),
                "safetySettings", List.of(Map.of("category", "HARM_CATEGORY_HARASSMENT", "threshold", "BLOCK_MEDIUM_AND_ABOVE"))
        );
        return webClient.post()
                .uri("/models/{model}:generateContent?key={key}", model, apiKey)
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
        return "gemini";
    }

    private String sanitize(String input) {
        return input == null ? "" : input.replaceAll("(?i)ignore previous instructions", "");
    }

    private String extractContent(Map<?, ?> response) {
        Object candidates = response.get("candidates");
        if (candidates instanceof List<?> candidateList && !candidateList.isEmpty()
                && candidateList.getFirst() instanceof Map<?, ?> candidate
                && candidate.get("content") instanceof Map<?, ?> content
                && content.get("parts") instanceof List<?> parts
                && !parts.isEmpty()
                && parts.getFirst() instanceof Map<?, ?> part
                && part.get("text") instanceof String text) {
            return text;
        }
        return response.toString();
    }
}
