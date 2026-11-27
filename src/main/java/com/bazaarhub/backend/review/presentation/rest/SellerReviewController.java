package com.bazaarhub.backend.review.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.review.application.dto.response.ReviewResponse;
import com.bazaarhub.backend.review.application.service.ProductReviewService;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/seller/reviews")
@Tag(name = "Seller Reviews", description = "Seller product review visibility and monitoring endpoints")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class SellerReviewController {

    private final ProductReviewService reviewService;

    public SellerReviewController(ProductReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('REVIEW_READ', 'SELLER_REVIEW_REPLY', 'ROLE_SELLER')")
    @Operation(summary = "Get seller product reviews", description = "Retrieves paginated reviews for products belonging exclusively to the authenticated seller")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller reviews retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid product ID filter", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires SELLER role", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getSellerReviews(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Optional filter by product ID") @RequestParam(required = false) Long productId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ReviewResponse> reviews = reviewService.getSellerProductReviews(principal.getId(), productId, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Seller reviews retrieved successfully", reviews));
    }
}
