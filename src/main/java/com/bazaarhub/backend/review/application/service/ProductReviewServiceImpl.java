package com.bazaarhub.backend.review.application.service;

import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductRepository;
import com.bazaarhub.backend.delivery.infrastructure.adapter.CloudinaryService;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderItemRepository;
import com.bazaarhub.backend.review.application.dto.request.CreateReviewRequest;
import com.bazaarhub.backend.review.application.dto.request.UpdateReviewRequest;
import com.bazaarhub.backend.review.application.dto.response.ProductRatingSummaryResponse;
import com.bazaarhub.backend.review.application.dto.response.ReviewResponse;
import com.bazaarhub.backend.review.application.mapper.ReviewMapper;
import com.bazaarhub.backend.review.domain.exception.DuplicateReviewException;
import com.bazaarhub.backend.review.domain.exception.IneligibleReviewException;
import com.bazaarhub.backend.review.domain.exception.InvalidReviewOperationException;
import com.bazaarhub.backend.review.domain.exception.ReviewNotFoundException;
import com.bazaarhub.backend.review.domain.exception.UnauthorizedReviewAccessException;
import com.bazaarhub.backend.review.domain.model.RatingSummary;
import com.bazaarhub.backend.review.domain.model.ReviewStatus;
import com.bazaarhub.backend.review.infrastructure.persistence.entity.ProductReviewEntity;
import com.bazaarhub.backend.review.infrastructure.persistence.entity.ReviewImageEntity;
import com.bazaarhub.backend.review.infrastructure.persistence.repository.SpringDataProductReviewRepository;
import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional
public class ProductReviewServiceImpl implements ProductReviewService {

    private static final Logger log = LoggerFactory.getLogger(ProductReviewServiceImpl.class);
    private static final int MAX_REVIEW_IMAGES = 5;

    private final SpringDataProductReviewRepository reviewRepository;
    private final SpringDataProductRepository productRepository;
    private final SpringDataUserRepository userRepository;
    private final SpringDataOrderItemRepository orderItemRepository;
    private final CloudinaryService cloudinaryService;
    private final ReviewMapper reviewMapper;

    public ProductReviewServiceImpl(SpringDataProductReviewRepository reviewRepository,
                                  SpringDataProductRepository productRepository,
                                  SpringDataUserRepository userRepository,
                                  SpringDataOrderItemRepository orderItemRepository,
                                  CloudinaryService cloudinaryService,
                                  ReviewMapper reviewMapper) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
        this.cloudinaryService = cloudinaryService;
        this.reviewMapper = reviewMapper;
    }

    @Override
    public ReviewResponse createReview(Long customerId, CreateReviewRequest request) {
        log.info("[REVIEW] Customer {} creating review for product {}", customerId, request.getProductId());

        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new InvalidReviewOperationException("Rating must be between 1 and 5 stars");
        }

        if (request.getImageUrls() != null && request.getImageUrls().size() > MAX_REVIEW_IMAGES) {
            throw new InvalidReviewOperationException("A review can contain at most " + MAX_REVIEW_IMAGES + " images");
        }

        ProductEntity product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()));

        UserEntity customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", customerId));

        // 1. Verify purchase & delivery status
        boolean hasPurchased = orderItemRepository.hasCustomerPurchasedAndReceivedProduct(customerId, request.getProductId());
        if (!hasPurchased) {
            log.warn("[REVIEW INELIGIBLE] Customer {} attempted to review product {} without a delivered order",
                    customerId, request.getProductId());
            throw new IneligibleReviewException(customerId, request.getProductId());
        }

        // 2. Enforce single review per customer per product
        if (reviewRepository.existsByProductIdAndUserId(request.getProductId(), customerId)) {
            log.warn("[REVIEW DUPLICATE] Customer {} already has a review for product {}", customerId, request.getProductId());
            throw new DuplicateReviewException(customerId, request.getProductId());
        }

        // 3. Build Review entity
        ProductReviewEntity review = ProductReviewEntity.builder()
                .product(product)
                .user(customer)
                .rating(request.getRating())
                .title(request.getTitle() != null ? request.getTitle().trim() : null)
                .reviewText(request.getReviewText().trim())
                .isVerifiedPurchase(true)
                .status(ReviewStatus.approved) // Auto-approve verified purchase reviews
                .createdBy(customer)
                .updatedBy(customer)
                .build();

        // 4. Attach review images if provided
        attachImages(review, request.getImageUrls(), customer);

        ProductReviewEntity saved = reviewRepository.save(review);
        log.info("[REVIEW CREATED] Review id={} successfully created by customer={} for product={}",
                saved.getId(), customerId, request.getProductId());

        return reviewMapper.toResponse(saved, false);
    }

    @Override
    public ReviewResponse updateReview(Long reviewId, Long customerId, UpdateReviewRequest request) {
        log.info("[REVIEW] Customer {} updating review {}", customerId, reviewId);

        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new InvalidReviewOperationException("Rating must be between 1 and 5 stars");
        }

        if (request.getImageUrls() != null && request.getImageUrls().size() > MAX_REVIEW_IMAGES) {
            throw new InvalidReviewOperationException("A review can contain at most " + MAX_REVIEW_IMAGES + " images");
        }

        ProductReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (!review.getUser().getId().equals(customerId)) {
            log.warn("[REVIEW UNAUTHORIZED] Customer {} attempted to edit review {} owned by user {}",
                    customerId, reviewId, review.getUser().getId());
            throw new UnauthorizedReviewAccessException(customerId, reviewId);
        }

        UserEntity customer = review.getUser();

        review.setRating(request.getRating());
        review.setTitle(request.getTitle() != null ? request.getTitle().trim() : null);
        review.setReviewText(request.getReviewText().trim());
        review.setUpdatedBy(customer);
        // Retain approved status on customer edits
        review.setStatus(ReviewStatus.approved);

        if (request.getImageUrls() != null) {
            review.clearImages();
            attachImages(review, request.getImageUrls(), customer);
        }

        ProductReviewEntity updated = reviewRepository.save(review);
        log.info("[REVIEW UPDATED] Review id={} successfully updated by customer={}", updated.getId(), customerId);

        return reviewMapper.toResponse(updated, false);
    }

    @Override
    public void deleteReview(Long reviewId, Long customerId) {
        log.info("[REVIEW] Customer {} deleting review {}", customerId, reviewId);

        ProductReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (!review.getUser().getId().equals(customerId)) {
            log.warn("[REVIEW UNAUTHORIZED] Customer {} attempted to delete review {} owned by user {}",
                    customerId, reviewId, review.getUser().getId());
            throw new UnauthorizedReviewAccessException(customerId, reviewId);
        }

        reviewRepository.delete(review);
        log.info("[REVIEW DELETED] Review id={} successfully deleted by customer={}", reviewId, customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getCustomerReviewById(Long reviewId, Long customerId) {
        ProductReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (!review.getUser().getId().equals(customerId)) {
            throw new UnauthorizedReviewAccessException(customerId, reviewId);
        }

        return reviewMapper.toResponse(review, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getCustomerReviews(Long customerId, Pageable pageable) {
        return reviewRepository.findByUserId(customerId, pageable)
                .map(entity -> reviewMapper.toResponse(entity, false));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getPublicProductReviews(Long productId, Integer rating, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", productId);
        }

        Page<ProductReviewEntity> reviews;
        if (rating != null && rating >= 1 && rating <= 5) {
            reviews = reviewRepository.findByProductIdAndStatusAndRating(productId, ReviewStatus.approved, rating, pageable);
        } else {
            reviews = reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.approved, pageable);
        }

        return reviews.map(entity -> reviewMapper.toResponse(entity, true));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductRatingSummaryResponse getProductRatingSummary(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", productId);
        }

        List<Object[]> aggregates = reviewRepository.getRatingAggregateByProductId(productId);
        if (aggregates == null || aggregates.isEmpty() || aggregates.get(0) == null) {
            return reviewMapper.toSummaryResponse(RatingSummary.builder()
                    .productId(productId)
                    .totalReviews(0)
                    .averageRating(0.0)
                    .fiveStarCount(0)
                    .fourStarCount(0)
                    .threeStarCount(0)
                    .twoStarCount(0)
                    .oneStarCount(0)
                    .build());
        }

        Object[] row = aggregates.get(0);
        long totalReviews = row[0] != null ? ((Number) row[0]).longValue() : 0L;
        double rawAvg = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
        double averageRating = BigDecimal.valueOf(rawAvg).setScale(1, RoundingMode.HALF_UP).doubleValue();
        long fiveStarCount = row[2] != null ? ((Number) row[2]).longValue() : 0L;
        long fourStarCount = row[3] != null ? ((Number) row[3]).longValue() : 0L;
        long threeStarCount = row[4] != null ? ((Number) row[4]).longValue() : 0L;
        long twoStarCount = row[5] != null ? ((Number) row[5]).longValue() : 0L;
        long oneStarCount = row[6] != null ? ((Number) row[6]).longValue() : 0L;

        RatingSummary summary = RatingSummary.builder()
                .productId(productId)
                .totalReviews(totalReviews)
                .averageRating(averageRating)
                .fiveStarCount(fiveStarCount)
                .fourStarCount(fourStarCount)
                .threeStarCount(threeStarCount)
                .twoStarCount(twoStarCount)
                .oneStarCount(oneStarCount)
                .build();

        return reviewMapper.toSummaryResponse(summary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getSellerProductReviews(Long sellerUserId, Long productId, Pageable pageable) {
        Page<ProductReviewEntity> reviews;
        if (productId != null) {
            reviews = reviewRepository.findBySellerUserIdAndProductId(sellerUserId, productId, pageable);
        } else {
            reviews = reviewRepository.findBySellerUserId(sellerUserId, pageable);
        }
        return reviews.map(entity -> reviewMapper.toResponse(entity, true));
    }

    private void attachImages(ProductReviewEntity review, List<String> imageUrls, UserEntity user) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        int order = 0;
        for (String rawImage : imageUrls) {
            if (rawImage == null || rawImage.isBlank()) {
                continue;
            }
            String uploadedUrl = cloudinaryService.uploadImage(rawImage, "bazaarhub/reviews");
            ReviewImageEntity imageEntity = ReviewImageEntity.builder()
                    .imageUrl(uploadedUrl != null ? uploadedUrl : rawImage)
                    .displayOrder(order++)
                    .createdBy(user)
                    .updatedBy(user)
                    .build();
            review.addImage(imageEntity);
        }
    }
}
