package com.bazaarhub.backend.review.application.dto.response;

import com.bazaarhub.backend.review.domain.model.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product review response payload")
public class ReviewResponse {

    @Schema(description = "Review ID", example = "10")
    private Long id;

    @Schema(description = "Associated product ID", example = "101")
    private Long productId;

    @Schema(description = "Product name snapshot", example = "Wireless Noise-Cancelling Headphones")
    private String productName;

    @Schema(description = "Rating score (1 to 5)", example = "5")
    private Integer rating;

    @Schema(description = "Review title / headline", example = "Outstanding audio clarity and comfort")
    private String title;

    @Schema(description = "Full review text", example = "Battery lasts over 30 hours, ANC is very effective for work.")
    private String reviewText;

    @Schema(description = "Whether the reviewer is a verified purchaser", example = "true")
    private boolean isVerifiedPurchase;

    @Schema(description = "Current moderation status", example = "approved")
    private ReviewStatus status;

    @Schema(description = "Privacy-safe display name of reviewer", example = "Ajay K.")
    private String reviewerName;

    @Builder.Default
    @Schema(description = "Attached review images")
    private List<ReviewImageResponse> images = new ArrayList<>();

    @Schema(description = "Review creation timestamp", example = "2026-11-14T10:00:00Z")
    private Instant createdAt;

    @Schema(description = "Review last update timestamp", example = "2026-11-14T10:00:00Z")
    private Instant updatedAt;
}
