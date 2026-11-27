package com.bazaarhub.backend.review.application.service;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.review.application.dto.request.ModerateReviewRequest;
import com.bazaarhub.backend.review.application.dto.response.ReviewResponse;
import com.bazaarhub.backend.review.application.mapper.ReviewMapper;
import com.bazaarhub.backend.review.domain.exception.ReviewNotFoundException;
import com.bazaarhub.backend.review.domain.model.ReviewStatus;
import com.bazaarhub.backend.review.infrastructure.persistence.entity.ProductReviewEntity;
import com.bazaarhub.backend.review.infrastructure.persistence.repository.SpringDataProductReviewRepository;
import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReviewModerationServiceImpl implements ReviewModerationService {

    private static final Logger log = LoggerFactory.getLogger(ReviewModerationServiceImpl.class);

    private final SpringDataProductReviewRepository reviewRepository;
    private final SpringDataUserRepository userRepository;
    private final ReviewMapper reviewMapper;

    public ReviewModerationServiceImpl(SpringDataProductReviewRepository reviewRepository,
                                       SpringDataUserRepository userRepository,
                                       ReviewMapper reviewMapper) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.reviewMapper = reviewMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getAllReviews(ReviewStatus status, Pageable pageable) {
        Page<ProductReviewEntity> reviews;
        if (status != null) {
            reviews = reviewRepository.findByStatus(status, pageable);
        } else {
            reviews = reviewRepository.findAll(pageable);
        }
        return reviews.map(entity -> reviewMapper.toResponse(entity, false));
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReviewById(Long reviewId) {
        ProductReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        return reviewMapper.toResponse(review, false);
    }

    @Override
    public ReviewResponse moderateReview(Long reviewId, Long adminId, ModerateReviewRequest request) {
        log.info("[ADMIN REVIEW MODERATION] Admin {} moderating review {} to status {}",
                adminId, reviewId, request.getStatus());

        ProductReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        UserEntity admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin User", adminId));

        review.setStatus(request.getStatus());
        review.setUpdatedBy(admin);

        ProductReviewEntity saved = reviewRepository.save(review);
        log.info("[ADMIN REVIEW MODERATED] Review {} status updated to {} by admin {}",
                reviewId, request.getStatus(), adminId);

        return reviewMapper.toResponse(saved, false);
    }

    @Override
    public void deleteReview(Long reviewId, Long adminId) {
        log.info("[ADMIN REVIEW DELETE] Admin {} deleting review {}", adminId, reviewId);

        ProductReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        reviewRepository.delete(review);
        log.info("[ADMIN REVIEW DELETED] Review {} deleted by admin {}", reviewId, adminId);
    }
}
