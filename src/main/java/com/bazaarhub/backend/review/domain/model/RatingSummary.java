package com.bazaarhub.backend.review.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Domain model representing dynamic aggregated rating metrics for a product.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingSummary {

    private Long productId;
    private long totalReviews;
    private double averageRating;
    private long fiveStarCount;
    private long fourStarCount;
    private long threeStarCount;
    private long twoStarCount;
    private long oneStarCount;

    public double getFiveStarPercentage() {
        return calculatePercentage(fiveStarCount);
    }

    public double getFourStarPercentage() {
        return calculatePercentage(fourStarCount);
    }

    public double getThreeStarPercentage() {
        return calculatePercentage(threeStarCount);
    }

    public double getTwoStarPercentage() {
        return calculatePercentage(twoStarCount);
    }

    public double getOneStarPercentage() {
        return calculatePercentage(oneStarCount);
    }

    private double calculatePercentage(long count) {
        if (totalReviews == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf((double) count * 100.0 / totalReviews)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
