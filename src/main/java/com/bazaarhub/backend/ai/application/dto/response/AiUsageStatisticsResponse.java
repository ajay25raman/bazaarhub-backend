package com.bazaarhub.backend.ai.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated metrics on AI token usage, costs, and invocation volumes")
public class AiUsageStatisticsResponse {

    @Schema(description = "Total AI API requests recorded", example = "1542")
    private long totalRequests;

    @Schema(description = "Total prompt/input tokens consumed", example = "350420")
    private long totalInputTokens;

    @Schema(description = "Total completion/output tokens generated", example = "128910")
    private long totalOutputTokens;

    @Schema(description = "Total tokens combined", example = "479330")
    private long totalTokens;

    @Schema(description = "Total estimated API cost in USD", example = "0.245800")
    private BigDecimal totalEstimatedCostUsd;

    @Schema(description = "Breakdown of requests by feature (e.g., SUPPORT_ASSISTANT, DESCRIPTION_GENERATOR)", example = "{\"SUPPORT_ASSISTANT\": 920, \"PRODUCT_RECOMMENDATIONS\": 380, \"DESCRIPTION_GENERATOR\": 180, \"REVIEW_SUMMARY\": 62}")
    private Map<String, Long> requestsByFeature;

    @Schema(description = "Configured AI provider", example = "groq")
    private String provider;

    @Schema(description = "Configured default model", example = "openai/gpt-oss-20b")
    private String defaultModel;
}
