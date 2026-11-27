package com.bazaarhub.backend.review;

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
import com.bazaarhub.backend.review.application.service.ProductReviewServiceImpl;
import com.bazaarhub.backend.review.domain.exception.DuplicateReviewException;
import com.bazaarhub.backend.review.domain.exception.IneligibleReviewException;
import com.bazaarhub.backend.review.domain.exception.InvalidReviewOperationException;
import com.bazaarhub.backend.review.domain.exception.ReviewNotFoundException;
import com.bazaarhub.backend.review.domain.exception.UnauthorizedReviewAccessException;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductReviewServiceUnitTest {

    @Mock
    private SpringDataProductReviewRepository reviewRepository;

    @Mock
    private SpringDataProductRepository productRepository;

    @Mock
    private SpringDataUserRepository userRepository;

    @Mock
    private SpringDataOrderItemRepository orderItemRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Spy
    private ReviewMapper reviewMapper = new ReviewMapper();

    @InjectMocks
    private ProductReviewServiceImpl reviewService;

    private UserEntity customer;
    private UserEntity anotherCustomer;
    private ProductEntity product;
    private ProductReviewEntity existingReview;

    @BeforeEach
    void setUp() {
        customer = UserEntity.builder()
                .id(1L)
                .fullName("Ajay Kumar")
                .email("ajay@example.com")
                .build();

        anotherCustomer = UserEntity.builder()
                .id(2L)
                .fullName("Vijay Sharma")
                .email("vijay@example.com")
                .build();

        product = ProductEntity.builder()
                .id(101L)
                .name("Wireless Noise Cancelling Headphones")
                .build();

        existingReview = ProductReviewEntity.builder()
                .id(10L)
                .product(product)
                .user(customer)
                .rating(5)
                .title("Superb audio clarity")
                .reviewText("Best ANC headphones in this segment.")
                .isVerifiedPurchase(true)
                .status(ReviewStatus.approved)
                .build();
    }

    @Test
    @DisplayName("Create review succeeds when customer has delivered purchase")
    void createReview_success() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .productId(101L)
                .rating(5)
                .title("Fantastic sound")
                .reviewText("The bass and mids are perfectly balanced.")
                .imageUrls(List.of("https://example.com/img1.jpg"))
                .build();

        when(productRepository.findById(101L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(orderItemRepository.hasCustomerPurchasedAndReceivedProduct(1L, 101L)).thenReturn(true);
        when(reviewRepository.existsByProductIdAndUserId(101L, 1L)).thenReturn(false);
        when(cloudinaryService.uploadImage(any(), any())).thenReturn("https://res.cloudinary.com/demo/image/upload/bazaarhub/reviews/mock.jpg");
        when(reviewRepository.save(any(ProductReviewEntity.class))).thenAnswer(invocation -> {
            ProductReviewEntity r = invocation.getArgument(0);
            r.setId(50L);
            return r;
        });

        ReviewResponse response = reviewService.createReview(1L, request);

        assertNotNull(response);
        assertEquals(50L, response.getId());
        assertEquals(101L, response.getProductId());
        assertEquals(5, response.getRating());
        assertEquals(ReviewStatus.approved, response.getStatus());
        assertTrue(response.isVerifiedPurchase());
        assertEquals("Ajay Kumar", response.getReviewerName());
        assertEquals(1, response.getImages().size());
        verify(reviewRepository).save(any(ProductReviewEntity.class));
    }

    @Test
    @DisplayName("Create review throws IneligibleReviewException when customer has not purchased or order not delivered")
    void createReview_ineligibleCustomer_throwsException() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .productId(101L)
                .rating(4)
                .title("Looks good")
                .reviewText("Haven't bought it yet but saw reviews.")
                .build();

        when(productRepository.findById(101L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(orderItemRepository.hasCustomerPurchasedAndReceivedProduct(1L, 101L)).thenReturn(false);

        assertThrows(IneligibleReviewException.class, () -> reviewService.createReview(1L, request));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create review throws DuplicateReviewException when customer already reviewed product")
    void createReview_duplicateReview_throwsException() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .productId(101L)
                .rating(5)
                .title("Second review")
                .reviewText("Writing another review for the same product.")
                .build();

        when(productRepository.findById(101L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(orderItemRepository.hasCustomerPurchasedAndReceivedProduct(1L, 101L)).thenReturn(true);
        when(reviewRepository.existsByProductIdAndUserId(101L, 1L)).thenReturn(true);

        assertThrows(DuplicateReviewException.class, () -> reviewService.createReview(1L, request));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create review throws InvalidReviewOperationException when rating is out of bounds")
    void createReview_invalidRating_throwsException() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .productId(101L)
                .rating(6)
                .title("Too high")
                .reviewText("Rating 6 stars is not allowed.")
                .build();

        assertThrows(InvalidReviewOperationException.class, () -> reviewService.createReview(1L, request));
    }

    @Test
    @DisplayName("Create review throws InvalidReviewOperationException when images exceed maximum limit of 5")
    void createReview_tooManyImages_throwsException() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .productId(101L)
                .rating(5)
                .title("Too many photos")
                .reviewText("Attached 6 photos.")
                .imageUrls(List.of("1", "2", "3", "4", "5", "6"))
                .build();

        assertThrows(InvalidReviewOperationException.class, () -> reviewService.createReview(1L, request));
    }

    @Test
    @DisplayName("Update review succeeds when customer is author and keeps approved status")
    void updateReview_success() {
        UpdateReviewRequest updateRequest = UpdateReviewRequest.builder()
                .rating(4)
                .title("Updated: Battery is good but takes time to charge")
                .reviewText("After 3 months of use, still works nicely.")
                .build();

        when(reviewRepository.findById(10L)).thenReturn(Optional.of(existingReview));
        when(reviewRepository.save(any(ProductReviewEntity.class))).thenReturn(existingReview);

        ReviewResponse response = reviewService.updateReview(10L, 1L, updateRequest);

        assertNotNull(response);
        assertEquals(4, existingReview.getRating());
        assertEquals("Updated: Battery is good but takes time to charge", existingReview.getTitle());
        assertEquals(ReviewStatus.approved, existingReview.getStatus());
        verify(reviewRepository).save(existingReview);
    }

    @Test
    @DisplayName("Update review throws UnauthorizedReviewAccessException when user is not author")
    void updateReview_unauthorizedUser_throwsException() {
        UpdateReviewRequest updateRequest = UpdateReviewRequest.builder()
                .rating(3)
                .title("Hacked title")
                .reviewText("Unauthorized edit attempt.")
                .build();

        when(reviewRepository.findById(10L)).thenReturn(Optional.of(existingReview));

        assertThrows(UnauthorizedReviewAccessException.class, () -> reviewService.updateReview(10L, 2L, updateRequest));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete review succeeds when user is author")
    void deleteReview_success() {
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(existingReview));

        reviewService.deleteReview(10L, 1L);

        verify(reviewRepository).delete(existingReview);
    }

    @Test
    @DisplayName("Delete review throws UnauthorizedReviewAccessException when user is not author")
    void deleteReview_unauthorizedUser_throwsException() {
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(existingReview));

        assertThrows(UnauthorizedReviewAccessException.class, () -> reviewService.deleteReview(10L, 2L));
        verify(reviewRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Product rating summary calculates dynamic average and star distribution")
    void getProductRatingSummary_success() {
        when(productRepository.existsById(101L)).thenReturn(true);
        // COUNT, AVG, 5-star, 4-star, 3-star, 2-star, 1-star
        Object[] aggregateRow = new Object[]{10L, 4.4, 6L, 2L, 2L, 0L, 0L};
        List<Object[]> rows = Collections.singletonList(aggregateRow);
        when(reviewRepository.getRatingAggregateByProductId(101L)).thenReturn(rows);

        ProductRatingSummaryResponse summary = reviewService.getProductRatingSummary(101L);

        assertNotNull(summary);
        assertEquals(101L, summary.getProductId());
        assertEquals(10L, summary.getTotalReviews());
        assertEquals(4.4, summary.getAverageRating());
        assertEquals(6L, summary.getFiveStarCount());
        assertEquals(2L, summary.getFourStarCount());
        assertEquals(2L, summary.getThreeStarCount());
        assertEquals(0L, summary.getTwoStarCount());
        assertEquals(0L, summary.getOneStarCount());
        assertEquals(60.0, summary.getFiveStarPercentage());
        assertEquals(20.0, summary.getFourStarPercentage());
        assertEquals(20.0, summary.getThreeStarPercentage());
    }

    @Test
    @DisplayName("Public reviews endpoint returns masked reviewer names for customer privacy")
    void getPublicProductReviews_masksReviewerName() {
        when(productRepository.existsById(101L)).thenReturn(true);
        Pageable pageable = PageRequest.of(0, 10);
        when(reviewRepository.findByProductIdAndStatus(101L, ReviewStatus.approved, pageable))
                .thenReturn(new PageImpl<>(List.of(existingReview), pageable, 1));

        Page<ReviewResponse> page = reviewService.getPublicProductReviews(101L, null, pageable);

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        ReviewResponse publicReview = page.getContent().get(0);
        assertEquals("Ajay K.", publicReview.getReviewerName());
    }
}
