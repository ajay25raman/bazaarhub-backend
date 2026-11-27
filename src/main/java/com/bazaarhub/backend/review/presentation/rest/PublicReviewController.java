package com.bazaarhub.backend.review.presentation.rest;

import com.bazaarhub.backend.review.application.dto.response.ProductRatingSummaryResponse;
import com.bazaarhub.backend.review.application.dto.response.ReviewResponse;
import com.bazaarhub.backend.review.application.service.ProductReviewService;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products/{productId}")
@Tag(name = "Product Reviews (Public)", description = "Public customer review browsing and rating summary endpoints")
public class PublicReviewController {

    private final ProductReviewService reviewService;

    public PublicReviewController(ProductReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/reviews")
    @Operation(summary = "Get public product reviews", description = "Retrieves paginated approved reviews for a product with privacy-masked customer names and optional star rating filter")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product reviews retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid rating filter", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getProductReviews(
            @Parameter(description = "Product ID", example = "1") @PathVariable Long productId,
            @Parameter(description = "Optional filter by star rating (1-5)") @RequestParam(required = false) Integer rating,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ReviewResponse> reviews = reviewService.getPublicProductReviews(productId, rating, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Product reviews retrieved successfully", reviews));
    }

    @GetMapping("/rating-summary")
    @Operation(summary = "Get product rating summary", description = "Retrieves real-time aggregated rating statistics, star score breakdown, and percentage distribution for a product")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product rating summary retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<ProductRatingSummaryResponse>> getProductRatingSummary(
            @Parameter(description = "Product ID", example = "1") @PathVariable Long productId) {

        ProductRatingSummaryResponse summary = reviewService.getProductRatingSummary(productId);
        return ResponseEntity.ok(ApiResponse.ok("Product rating summary retrieved successfully", summary));
    }
}
