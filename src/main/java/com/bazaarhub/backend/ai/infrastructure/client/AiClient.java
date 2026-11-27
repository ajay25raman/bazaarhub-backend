package com.bazaarhub.backend.ai.infrastructure.client;

import java.util.List;

public interface AiClient {

    AiCompletionResponse completeChat(List<AiChatMessage> messages, int maxTokens, double temperature);

    boolean isEnabled();

    String getProviderName();

    String getModelName();
}
