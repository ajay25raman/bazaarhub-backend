package com.bazaarhub.backend.review.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated product rating summary and distribution statistics")
public class ProductRatingSummaryResponse {

    @Schema(description = "Product ID", example = "101")
    private Long productId;

    @Schema(description = "Average rating score rounded to one decimal place", example = "4.6")
    private double averageRating;

    @Schema(description = "Total number of approved reviews", example = "128")
    private long totalReviews;

    @Schema(description = "Count of 5-star ratings", example = "85")
    private long fiveStarCount;

    @Schema(description = "Count of 4-star ratings", example = "30")
    private long fourStarCount;

    @Schema(description = "Count of 3-star ratings", example = "8")
    private long threeStarCount;

    @Schema(description = "Count of 2-star ratings", example = "3")
    private long twoStarCount;

    @Schema(description = "Count of 1-star ratings", example = "2")
    private long oneStarCount;

    @Schema(description = "Percentage of 5-star ratings", example = "66.4")
    private double fiveStarPercentage;

    @Schema(description = "Percentage of 4-star ratings", example = "23.4")
    private double fourStarPercentage;

    @Schema(description = "Percentage of 3-star ratings", example = "6.3")
    private double threeStarPercentage;

    @Schema(description = "Percentage of 2-star ratings", example = "2.3")
    private double twoStarPercentage;

    @Schema(description = "Percentage of 1-star ratings", example = "1.6")
    private double oneStarPercentage;
}
