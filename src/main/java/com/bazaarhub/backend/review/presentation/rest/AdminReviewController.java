package com.bazaarhub.backend.review.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.review.application.dto.request.ModerateReviewRequest;
import com.bazaarhub.backend.review.application.dto.response.ReviewResponse;
import com.bazaarhub.backend.review.application.service.ReviewModerationService;
import com.bazaarhub.backend.review.domain.model.ReviewStatus;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@Tag(name = "Admin Review Moderation", description = "Admin product review moderation and management endpoints")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class AdminReviewController {

    private final ReviewModerationService moderationService;

    public AdminReviewController(ReviewModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('REVIEW_MODERATE', 'ROLE_ADMIN')")
    @Operation(summary = "Get all reviews", description = "Retrieves paginated reviews across the entire platform, with optional filter by status")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Platform reviews retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires REVIEW_MODERATE authority", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getAllReviews(
            @Parameter(description = "Optional filter by status (pending, approved, rejected)") @RequestParam(required = false) ReviewStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ReviewResponse> reviews = moderationService.getAllReviews(status, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Platform reviews retrieved successfully", reviews));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('REVIEW_MODERATE', 'ROLE_ADMIN')")
    @Operation(summary = "Get review details", description = "Retrieves full review details by ID for admin audit")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Review details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires REVIEW_MODERATE authority", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Review not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<ReviewResponse>> getReviewById(
            @Parameter(description = "Review ID", example = "1") @PathVariable Long id) {
        ReviewResponse review = moderationService.getReviewById(id);
        return ResponseEntity.ok(ApiResponse.ok("Review details retrieved successfully", review));
    }

    @PatchMapping("/{id}/moderate")
    @PreAuthorize("hasAnyAuthority('REVIEW_MODERATE', 'ROLE_ADMIN')")
    @Operation(summary = "Moderate product review", description = "Approves or rejects a customer review")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Review moderated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires REVIEW_MODERATE authority", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Review not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<ReviewResponse>> moderateReview(
            @Parameter(description = "Review ID", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ModerateReviewRequest request) {

        ReviewResponse response = moderationService.moderateReview(id, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Review moderated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('REVIEW_MODERATE', 'ROLE_ADMIN')")
    @Operation(summary = "Delete review", description = "Permanently deletes a product review from the platform")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Review deleted successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires REVIEW_MODERATE authority", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Review not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @Parameter(description = "Review ID", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {

        moderationService.deleteReview(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Review deleted successfully", null));
    }
}
