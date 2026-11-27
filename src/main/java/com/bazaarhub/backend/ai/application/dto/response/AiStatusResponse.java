package com.bazaarhub.backend.ai.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Operational health and configuration status of AI provider")
public class AiStatusResponse {

    @Schema(description = "Whether AI capability is administratively enabled", example = "true")
    private boolean enabled;

    @Schema(description = "Active AI provider name", example = "groq")
    private String provider;

    @Schema(description = "Active AI model identifier", example = "openai/gpt-oss-20b")
    private String model;

    @Schema(description = "Base API URL for provider", example = "https://api.groq.com/openai/v1")
    private String baseUrl;

    @Schema(description = "Whether API key is present in environment", example = "true")
    private boolean apiKeyConfigured;

    @Schema(description = "Request timeout in milliseconds", example = "30000")
    private int timeoutMs;

    @Schema(description = "Maximum output tokens limit", example = "1000")
    private int maxOutputTokens;
}
