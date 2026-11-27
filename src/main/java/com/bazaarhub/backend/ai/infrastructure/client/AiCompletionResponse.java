package com.bazaarhub.backend.ai.infrastructure.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCompletionResponse {
    private String content;
    private String model;
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
    private long latencyMs;
}
