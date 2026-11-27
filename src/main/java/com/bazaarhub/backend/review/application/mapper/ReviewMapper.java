package com.bazaarhub.backend.review.application.mapper;

import com.bazaarhub.backend.review.application.dto.response.ProductRatingSummaryResponse;
import com.bazaarhub.backend.review.application.dto.response.ReviewImageResponse;
import com.bazaarhub.backend.review.application.dto.response.ReviewResponse;
import com.bazaarhub.backend.review.domain.model.RatingSummary;
import com.bazaarhub.backend.review.infrastructure.persistence.entity.ProductReviewEntity;
import com.bazaarhub.backend.review.infrastructure.persistence.entity.ReviewImageEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ReviewMapper {

    public ReviewResponse toResponse(ProductReviewEntity entity, boolean maskReviewerName) {
        if (entity == null) {
            return null;
        }

        String displayName = "Customer";
        if (entity.getUser() != null) {
            displayName = maskReviewerName 
                    ? maskName(entity.getUser().getFullName()) 
                    : entity.getUser().getFullName();
        }

        String productName = null;
        if (entity.getProduct() != null) {
            productName = entity.getProduct().getName();
        }

        List<ReviewImageResponse> imageResponses = entity.getImages() != null
                ? entity.getImages().stream().map(this::toImageResponse).collect(Collectors.toList())
                : Collections.emptyList();

        return ReviewResponse.builder()
                .id(entity.getId())
                .productId(entity.getProduct() != null ? entity.getProduct().getId() : null)
                .productName(productName)
                .rating(entity.getRating())
                .title(entity.getTitle())
                .reviewText(entity.getReviewText())
                .isVerifiedPurchase(entity.isVerifiedPurchase())
                .status(entity.getStatus())
                .reviewerName(displayName)
                .images(imageResponses)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ReviewImageResponse toImageResponse(ReviewImageEntity entity) {
        if (entity == null) {
            return null;
        }
        return ReviewImageResponse.builder()
                .id(entity.getId())
                .imageUrl(entity.getImageUrl())
                .displayOrder(entity.getDisplayOrder())
                .build();
    }

    public ProductRatingSummaryResponse toSummaryResponse(RatingSummary summary) {
        if (summary == null) {
            return null;
        }
        return ProductRatingSummaryResponse.builder()
                .productId(summary.getProductId())
                .averageRating(summary.getAverageRating())
                .totalReviews(summary.getTotalReviews())
                .fiveStarCount(summary.getFiveStarCount())
                .fourStarCount(summary.getFourStarCount())
                .threeStarCount(summary.getThreeStarCount())
                .twoStarCount(summary.getTwoStarCount())
                .oneStarCount(summary.getOneStarCount())
                .fiveStarPercentage(summary.getFiveStarPercentage())
                .fourStarPercentage(summary.getFourStarPercentage())
                .threeStarPercentage(summary.getThreeStarPercentage())
                .twoStarPercentage(summary.getTwoStarPercentage())
                .oneStarPercentage(summary.getOneStarPercentage())
                .build();
    }

    public static String maskName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Customer";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        String firstName = parts[0];
        String lastInitial = parts[parts.length - 1].substring(0, 1).toUpperCase();
        return firstName + " " + lastInitial + ".";
    }
}
