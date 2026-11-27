package com.bazaarhub.backend.ai.infrastructure.client;

import com.bazaarhub.backend.ai.domain.exception.AiDisabledException;
import com.bazaarhub.backend.ai.domain.exception.AiRateLimitException;
import com.bazaarhub.backend.ai.domain.exception.AiServiceException;
import com.bazaarhub.backend.ai.domain.exception.AiTimeoutException;
import com.bazaarhub.backend.ai.domain.model.AiUsageStatus;
import com.bazaarhub.backend.ai.infrastructure.config.AiConfigurationProperties;
import com.bazaarhub.backend.ai.infrastructure.persistence.entity.AiUsageLogEntity;
import com.bazaarhub.backend.ai.infrastructure.persistence.repository.SpringDataAiUsageLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GroqAiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(GroqAiClient.class);

    private final AiConfigurationProperties properties;
    private final SpringDataAiUsageLogRepository usageLogRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GroqAiClient(AiConfigurationProperties properties,
                        SpringDataAiUsageLogRepository usageLogRepository,
                        ObjectMapper objectMapper) {
        this.properties = properties;
        this.usageLogRepository = usageLogRepository;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeout = properties.getTimeoutMs() > 0 ? properties.getTimeoutMs() : 30000;
        requestFactory.setConnectTimeout(Duration.ofMillis(Math.min(timeout, 10000)));
        requestFactory.setReadTimeout(Duration.ofMillis(timeout));

        String baseUrl = properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank()
                ? properties.getBaseUrl()
                : "https://api.groq.com/openai/v1";

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public String getProviderName() {
        return properties.getProvider() != null ? properties.getProvider() : "groq";
    }

    @Override
    public String getModelName() {
        return properties.getModel() != null ? properties.getModel() : "openai/gpt-oss-20b";
    }

    @Override
    public AiCompletionResponse completeChat(List<AiChatMessage> messages, int maxTokens, double temperature) {
        if (!properties.isEnabled()) {
            throw new AiDisabledException("AI capabilities are currently administratively disabled");
        }

        String apiKey = properties.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty() || "placeholder_ai_key".equals(apiKey)) {
            throw new AiDisabledException("AI API key is missing or not configured in environment (AI_API_KEY)");
        }

        int boundedMaxTokens = maxTokens > 0
                ? Math.min(maxTokens, properties.getMaxOutputTokens())
                : properties.getMaxOutputTokens();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", getModelName());
        requestBody.put("temperature", Math.max(0.0, Math.min(temperature, 1.0)));
        requestBody.put("max_tokens", boundedMaxTokens);

        List<Map<String, String>> formattedMessages = new ArrayList<>();
        for (AiChatMessage msg : messages) {
            formattedMessages.add(Map.of(
                    "role", msg.getRole(),
                    "content", msg.getContent()
            ));
        }
        requestBody.put("messages", formattedMessages);

        long startTime = System.currentTimeMillis();
        try {
            log.info("[GROQ AI REQUEST] Sending completion request to Groq for model: {}", getModelName());

            String rawResponse = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            long latencyMs = System.currentTimeMillis() - startTime;
            JsonNode root = objectMapper.readTree(rawResponse);

            String content = root.path("choices").path(0).path("message").path("content").asText("");
            String responseModel = root.path("model").asText(getModelName());

            int promptTokens = root.path("usage").path("prompt_tokens").asInt(0);
            int completionTokens = root.path("usage").path("completion_tokens").asInt(0);
            int totalTokens = root.path("usage").path("total_tokens").asInt(promptTokens + completionTokens);

            log.info("[GROQ AI SUCCESS] Response received in {}ms. Tokens: {} (prompt: {}, completion: {})",
                    latencyMs, totalTokens, promptTokens, completionTokens);

            // Record usage log
            recordUsage("CHAT_COMPLETION", promptTokens, completionTokens, AiUsageStatus.SUCCESS, null);

            return AiCompletionResponse.builder()
                    .content(content.trim())
                    .model(responseModel)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(totalTokens)
                    .latencyMs(latencyMs)
                    .build();

        } catch (RestClientResponseException ex) {
            long latencyMs = System.currentTimeMillis() - startTime;
            HttpStatusCode status = ex.getStatusCode();
            log.error("[GROQ AI ERROR] HTTP {} returned by Groq API in {}ms: {}", status.value(), latencyMs, ex.getResponseBodyAsString());

            recordUsage("CHAT_COMPLETION", 0, 0, AiUsageStatus.FAILED, "HTTP " + status.value() + ": " + ex.getStatusText());

            if (status.value() == 429) {
                Long retryAfterSeconds = null;
                String retryHeader = ex.getResponseHeaders() != null ? ex.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER) : null;
                if (retryHeader != null) {
                    try {
                        retryAfterSeconds = Long.parseLong(retryHeader.trim());
                    } catch (NumberFormatException ignored) {}
                }
                throw new AiRateLimitException("Groq AI API rate limit reached. Please retry after some time.", retryAfterSeconds);
            }

            throw new AiServiceException("Groq AI service failed with HTTP " + status.value() + ": " + ex.getStatusText(), ex);

        } catch (ResourceAccessException ex) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.error("[GROQ AI TIMEOUT/NETWORK] Connection or read timeout to Groq API after {}ms: {}", latencyMs, ex.getMessage());
            recordUsage("CHAT_COMPLETION", 0, 0, AiUsageStatus.FAILED, "Timeout/Network error: " + ex.getMessage());
            throw new AiTimeoutException("Groq AI service request timed out after " + properties.getTimeoutMs() + "ms");

        } catch (AiDisabledException | AiRateLimitException | AiTimeoutException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[GROQ AI UNEXPECTED ERROR] Failed executing completion: {}", ex.getMessage(), ex);
            recordUsage("CHAT_COMPLETION", 0, 0, AiUsageStatus.FAILED, ex.getMessage());
            throw new AiServiceException("Unexpected error during AI completion: " + ex.getMessage(), ex);
        }
    }

    private void recordUsage(String feature, int inputTokens, int outputTokens, AiUsageStatus status, String errorMessage) {
        try {
            // Groq typical pricing estimation: ~$0.15 / 1M prompt tokens, ~$0.60 / 1M completion tokens
            BigDecimal inputCost = BigDecimal.valueOf(inputTokens)
                    .multiply(BigDecimal.valueOf(0.00000015));
            BigDecimal outputCost = BigDecimal.valueOf(outputTokens)
                    .multiply(BigDecimal.valueOf(0.00000060));
            BigDecimal totalCost = inputCost.add(outputCost).setScale(6, RoundingMode.HALF_UP);

            AiUsageLogEntity logEntity = AiUsageLogEntity.builder()
                    .provider(getProviderName())
                    .modelName(getModelName())
                    .featureName(feature)
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
                    .estimatedCost(totalCost)
                    .currency("USD")
                    .status(status)
                    .errorMessage(errorMessage != null && errorMessage.length() > 950 ? errorMessage.substring(0, 950) : errorMessage)
                    .build();

            usageLogRepository.save(logEntity);
        } catch (Exception ex) {
            log.warn("[AI USAGE TRACKING WARN] Could not persist AI usage log: {}", ex.getMessage());
        }
    }
}
