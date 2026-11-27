package com.bazaarhub.backend.ai.presentation.rest;

import com.bazaarhub.backend.ai.application.dto.request.AiChatRequest;
import com.bazaarhub.backend.ai.application.dto.response.AiChatResponse;
import com.bazaarhub.backend.ai.application.dto.response.AiProductRecommendationResponse;
import com.bazaarhub.backend.ai.application.dto.response.AiReviewSummaryResponse;
import com.bazaarhub.backend.ai.application.service.AiProductRecommendationService;
import com.bazaarhub.backend.ai.application.service.AiReviewSummaryService;
import com.bazaarhub.backend.ai.application.service.AiSupportAssistantService;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/ai")
@Tag(name = "Public AI Services", description = "Public endpoints for marketplace recommendations, review sentiment summaries, and guest support guidance")
public class PublicAiController {

    private final AiSupportAssistantService assistantService;
    private final AiProductRecommendationService recommendationService;
    private final AiReviewSummaryService reviewSummaryService;

    public PublicAiController(AiSupportAssistantService assistantService,
                              AiProductRecommendationService recommendationService,
                              AiReviewSummaryService reviewSummaryService) {
        this.assistantService = assistantService;
        this.recommendationService = recommendationService;
        this.reviewSummaryService = reviewSummaryService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Guest inquiry chat with BazaarHub AI Assistant", description = "Answers general marketplace FAQs regarding shipping, returns, payment options, and shopping policies")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "AI guidance response generated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid message content", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "AI assistant service error", content = @Content)
    })
    public ResponseEntity<ApiResponse<AiChatResponse>> publicChat(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AiChatRequest request) {

        Long userId = principal != null ? principal.getId() : null;
        AiChatResponse response = assistantService.chat(request, userId);
        return ResponseEntity.ok(ApiResponse.ok("AI response generated", response));
    }

    @GetMapping("/recommendations")
    @Operation(summary = "AI-powered product recommendations", description = "Fetches active, approved, in-stock products with contextual recommendation rationale and verified pricing")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recommendations retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid category ID or limit parameters", content = @Content)
    })
    public ResponseEntity<ApiResponse<List<AiProductRecommendationResponse>>> getRecommendations(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Category filter ID", example = "1") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Maximum recommendations to return", example = "10") @RequestParam(defaultValue = "10") int limit) {

        Long userId = principal != null ? principal.getId() : null;
        List<AiProductRecommendationResponse> recommendations = recommendationService.getRecommendations(categoryId, userId, limit);
        return ResponseEntity.ok(ApiResponse.ok("Recommendations retrieved", recommendations));
    }

    @GetMapping("/products/{productId}/review-summary")
    @Operation(summary = "AI product review sentiment synthesis", description = "Extracts positive themes, negative themes, recurring defect flags, and consensus from verified approved reviews")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Review sentiment summary generated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found or has no published reviews", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "AI synthesis failure", content = @Content)
    })
    public ResponseEntity<ApiResponse<AiReviewSummaryResponse>> getReviewSummary(
            @Parameter(description = "Product ID", example = "1") @PathVariable Long productId) {
        AiReviewSummaryResponse summary = reviewSummaryService.summarizeProductReviews(productId);
        return ResponseEntity.ok(ApiResponse.ok("Review summary generated", summary));
    }
}
