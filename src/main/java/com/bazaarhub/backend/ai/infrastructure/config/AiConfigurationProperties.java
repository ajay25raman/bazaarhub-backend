package com.bazaarhub.backend.ai.infrastructure.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class AiConfigurationProperties {

    @Value("${bazaarhub.ai.enabled:${AI_ENABLED:true}}")
    private boolean enabled;

    @Value("${bazaarhub.ai.provider:${AI_PROVIDER:groq}}")
    private String provider;

    @Value("${bazaarhub.ai.model:${AI_MODEL:openai/gpt-oss-20b}}")
    private String model;

    @Value("${bazaarhub.ai.base-url:${AI_BASE_URL:https://api.groq.com/openai/v1}}")
    private String baseUrl;

    @Value("${bazaarhub.ai.api-key:${AI_API_KEY:}}")
    private String apiKey;

    @Value("${bazaarhub.ai.timeout-ms:${AI_TIMEOUT:30000}}")
    private int timeoutMs;

    @Value("${bazaarhub.ai.max-output-tokens:${AI_MAX_OUTPUT_TOKENS:1000}}")
    private int maxOutputTokens;
}
