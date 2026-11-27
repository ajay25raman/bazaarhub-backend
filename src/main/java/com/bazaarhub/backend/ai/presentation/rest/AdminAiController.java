package com.bazaarhub.backend.ai.presentation.rest;

import com.bazaarhub.backend.ai.application.dto.request.AiGenerateDescriptionRequest;
import com.bazaarhub.backend.ai.application.dto.response.AiProductDescriptionResponse;
import com.bazaarhub.backend.ai.application.dto.response.AiStatusResponse;
import com.bazaarhub.backend.ai.application.dto.response.AiUsageStatisticsResponse;
import com.bazaarhub.backend.ai.application.service.AiProductDescriptionService;
import com.bazaarhub.backend.ai.infrastructure.client.AiClient;
import com.bazaarhub.backend.ai.infrastructure.config.AiConfigurationProperties;
import com.bazaarhub.backend.ai.infrastructure.persistence.repository.SpringDataAiUsageLogRepository;
import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/ai")
@Tag(name = "Admin AI Management", description = "Administrative endpoints for monitoring AI consumption, token metrics, model configuration, and catalog tools")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiController {

    private final AiConfigurationProperties properties;
    private final AiClient aiClient;
    private final SpringDataAiUsageLogRepository usageLogRepository;
    private final AiProductDescriptionService descriptionService;

    public AdminAiController(AiConfigurationProperties properties,
                             AiClient aiClient,
                             SpringDataAiUsageLogRepository usageLogRepository,
                             AiProductDescriptionService descriptionService) {
        this.properties = properties;
        this.aiClient = aiClient;
        this.usageLogRepository = usageLogRepository;
        this.descriptionService = descriptionService;
    }

    @GetMapping("/status")
    @Operation(summary = "Check AI operational health and provider settings", description = "Returns active provider, model name, timeout, and API key availability")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "AI operational status retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN authority", content = @Content)
    })
    public ResponseEntity<ApiResponse<AiStatusResponse>> getStatus() {
        boolean apiKeyConfigured = properties.getApiKey() != null && !properties.getApiKey().isBlank();
        AiStatusResponse status = AiStatusResponse.builder()
                .enabled(properties.isEnabled())
                .provider(aiClient.getProviderName())
                .model(aiClient.getModelName())
                .baseUrl(properties.getBaseUrl())
                .apiKeyConfigured(apiKeyConfigured)
                .timeoutMs(properties.getTimeoutMs())
                .maxOutputTokens(properties.getMaxOutputTokens())
                .build();

        return ResponseEntity.ok(ApiResponse.ok("AI status retrieved", status));
    }

    @GetMapping("/usage-statistics")
    @Operation(summary = "Retrieve AI token usage and estimated cost analytics", description = "Aggregated token metrics, costs, and invocation breakdown across features")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "AI usage statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid days parameter", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN authority", content = @Content)
    })
    public ResponseEntity<ApiResponse<AiUsageStatisticsResponse>> getUsageStatistics(
            @Parameter(description = "Aggregation period in days", example = "30") @RequestParam(defaultValue = "30") int days) {

        Instant startDate = Instant.now().minus(days, ChronoUnit.DAYS);
        Instant endDate = Instant.now();

        long totalRequests = usageLogRepository.countLogsBetween(startDate, endDate);
        long totalInputTokens = usageLogRepository.sumInputTokensBetween(startDate, endDate);
        long totalOutputTokens = usageLogRepository.sumOutputTokensBetween(startDate, endDate);
        BigDecimal totalCost = usageLogRepository.sumEstimatedCostBetween(startDate, endDate);

        List<Object[]> featureAgg = usageLogRepository.aggregateUsageByFeature(startDate, endDate);
        Map<String, Long> requestsByFeature = new HashMap<>();
        if (featureAgg != null) {
            for (Object[] row : featureAgg) {
                String feature = (String) row[0];
                Long count = ((Number) row[1]).longValue();
                requestsByFeature.put(feature, count);
            }
        }

        AiUsageStatisticsResponse response = AiUsageStatisticsResponse.builder()
                .totalRequests(totalRequests)
                .totalInputTokens(totalInputTokens)
                .totalOutputTokens(totalOutputTokens)
                .totalTokens(totalInputTokens + totalOutputTokens)
                .totalEstimatedCostUsd(totalCost)
                .requestsByFeature(requestsByFeature)
                .provider(aiClient.getProviderName())
                .defaultModel(aiClient.getModelName())
                .build();

        return ResponseEntity.ok(ApiResponse.ok("AI usage statistics retrieved", response));
    }

    @PostMapping("/generate-description")
    @Operation(summary = "Admin catalog description generator", description = "Generates high-converting listing drafts for catalog moderation and administrative curation")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Draft listing generated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid input parameters", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN authority", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "AI provider invocation failure or rate limit", content = @Content)
    })
    public ResponseEntity<ApiResponse<AiProductDescriptionResponse>> generateDescription(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AiGenerateDescriptionRequest request) {

        AiProductDescriptionResponse response = descriptionService.generateContent(request, principal != null ? principal.getId() : null);
        return ResponseEntity.ok(ApiResponse.ok("Draft listing generated", response));
    }
}
