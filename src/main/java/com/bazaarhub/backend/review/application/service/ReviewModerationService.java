package com.bazaarhub.backend.review.application.service;

import com.bazaarhub.backend.review.application.dto.request.ModerateReviewRequest;
import com.bazaarhub.backend.review.application.dto.response.ReviewResponse;
import com.bazaarhub.backend.review.domain.model.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewModerationService {

    Page<ReviewResponse> getAllReviews(ReviewStatus status, Pageable pageable);

    ReviewResponse getReviewById(Long reviewId);

    ReviewResponse moderateReview(Long reviewId, Long adminId, ModerateReviewRequest request);

    void deleteReview(Long reviewId, Long adminId);
}
