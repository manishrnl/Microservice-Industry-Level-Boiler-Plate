package com.company.platform.ai.provider;

import java.util.List;

public record ChatRequest(String sessionId, String message, String systemPrompt,
                          List<ChatTurn> history) {
    public record ChatTurn(String role, String content) {
    }
}
