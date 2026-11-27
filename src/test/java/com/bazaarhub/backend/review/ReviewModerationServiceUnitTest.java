package com.bazaarhub.backend.review;

import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.review.application.dto.request.ModerateReviewRequest;
import com.bazaarhub.backend.review.application.dto.response.ReviewResponse;
import com.bazaarhub.backend.review.application.mapper.ReviewMapper;
import com.bazaarhub.backend.review.application.service.ReviewModerationServiceImpl;
import com.bazaarhub.backend.review.domain.exception.ReviewNotFoundException;
import com.bazaarhub.backend.review.domain.model.ReviewStatus;
import com.bazaarhub.backend.review.infrastructure.persistence.entity.ProductReviewEntity;
import com.bazaarhub.backend.review.infrastructure.persistence.repository.SpringDataProductReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewModerationServiceUnitTest {

    @Mock
    private SpringDataProductReviewRepository reviewRepository;

    @Mock
    private SpringDataUserRepository userRepository;

    @Spy
    private ReviewMapper reviewMapper = new ReviewMapper();

    @InjectMocks
    private ReviewModerationServiceImpl moderationService;

    private UserEntity adminUser;
    private ProductReviewEntity pendingReview;

    @BeforeEach
    void setUp() {
        adminUser = UserEntity.builder()
                .id(99L)
                .fullName("Admin Moderation Officer")
                .email("admin@bazaarhub.com")
                .build();

        ProductEntity product = ProductEntity.builder()
                .id(200L)
                .name("Smart Fitness Band")
                .build();

        UserEntity reviewer = UserEntity.builder()
                .id(5L)
                .fullName("Priya Patel")
                .email("priya@example.com")
                .build();

        pendingReview = ProductReviewEntity.builder()
                .id(25L)
                .product(product)
                .user(reviewer)
                .rating(4)
                .title("Good step counter")
                .reviewText("Accurate sensors and nice battery backup.")
                .status(ReviewStatus.pending)
                .isVerifiedPurchase(true)
                .build();
    }

    @Test
    @DisplayName("Admin retrieves all reviews with status filter")
    void getAllReviews_withStatusFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        when(reviewRepository.findByStatus(ReviewStatus.pending, pageable))
                .thenReturn(new PageImpl<>(List.of(pendingReview), pageable, 1));

        Page<ReviewResponse> page = moderationService.getAllReviews(ReviewStatus.pending, pageable);

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals(ReviewStatus.pending, page.getContent().get(0).getStatus());
        verify(reviewRepository).findByStatus(ReviewStatus.pending, pageable);
    }

    @Test
    @DisplayName("Admin moderates review to approved")
    void moderateReview_approve_success() {
        ModerateReviewRequest request = ModerateReviewRequest.builder()
                .status(ReviewStatus.approved)
                .reason("Complies with guidelines")
                .build();

        when(reviewRepository.findById(25L)).thenReturn(Optional.of(pendingReview));
        when(userRepository.findById(99L)).thenReturn(Optional.of(adminUser));
        when(reviewRepository.save(any(ProductReviewEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = moderationService.moderateReview(25L, 99L, request);

        assertNotNull(response);
        assertEquals(ReviewStatus.approved, response.getStatus());
        assertEquals(adminUser, pendingReview.getUpdatedBy());
        verify(reviewRepository).save(pendingReview);
    }

    @Test
    @DisplayName("Admin moderates review to rejected")
    void moderateReview_reject_success() {
        ModerateReviewRequest request = ModerateReviewRequest.builder()
                .status(ReviewStatus.rejected)
                .reason("Contains inappropriate language or external links")
                .build();

        when(reviewRepository.findById(25L)).thenReturn(Optional.of(pendingReview));
        when(userRepository.findById(99L)).thenReturn(Optional.of(adminUser));
        when(reviewRepository.save(any(ProductReviewEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = moderationService.moderateReview(25L, 99L, request);

        assertNotNull(response);
        assertEquals(ReviewStatus.rejected, response.getStatus());
        assertEquals(adminUser, pendingReview.getUpdatedBy());
        verify(reviewRepository).save(pendingReview);
    }

    @Test
    @DisplayName("Admin delete review succeeds")
    void deleteReview_success() {
        when(reviewRepository.findById(25L)).thenReturn(Optional.of(pendingReview));

        moderationService.deleteReview(25L, 99L);

        verify(reviewRepository).delete(pendingReview);
    }

    @Test
    @DisplayName("Get review by ID throws ReviewNotFoundException when ID does not exist")
    void getReviewById_notFound_throwsException() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ReviewNotFoundException.class, () -> moderationService.getReviewById(999L));
    }
}
