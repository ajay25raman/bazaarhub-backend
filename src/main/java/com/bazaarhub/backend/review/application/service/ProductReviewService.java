package com.bazaarhub.backend.review.application.service;

import com.bazaarhub.backend.review.application.dto.request.CreateReviewRequest;
import com.bazaarhub.backend.review.application.dto.request.UpdateReviewRequest;
import com.bazaarhub.backend.review.application.dto.response.ProductRatingSummaryResponse;
import com.bazaarhub.backend.review.application.dto.response.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductReviewService {

    ReviewResponse createReview(Long customerId, CreateReviewRequest request);

    ReviewResponse updateReview(Long reviewId, Long customerId, UpdateReviewRequest request);

    void deleteReview(Long reviewId, Long customerId);

    ReviewResponse getCustomerReviewById(Long reviewId, Long customerId);

    Page<ReviewResponse> getCustomerReviews(Long customerId, Pageable pageable);

    Page<ReviewResponse> getPublicProductReviews(Long productId, Integer rating, Pageable pageable);

    ProductRatingSummaryResponse getProductRatingSummary(Long productId);

    Page<ReviewResponse> getSellerProductReviews(Long sellerUserId, Long productId, Pageable pageable);
}
