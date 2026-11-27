package com.bazaarhub.backend.ai;

import com.bazaarhub.backend.ai.application.dto.response.AiReviewSummaryResponse;
import com.bazaarhub.backend.ai.application.service.AiReviewSummaryServiceImpl;
import com.bazaarhub.backend.ai.infrastructure.client.AiClient;
import com.bazaarhub.backend.ai.infrastructure.client.AiCompletionResponse;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.review.domain.model.ReviewStatus;
import com.bazaarhub.backend.review.infrastructure.persistence.entity.ProductReviewEntity;
import com.bazaarhub.backend.review.infrastructure.persistence.repository.SpringDataProductReviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiReviewSummaryServiceUnitTest {

    @Mock
    private SpringDataProductReviewRepository reviewRepository;

    @Mock
    private AiClient aiClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AiReviewSummaryServiceImpl service;

    @Test
    @DisplayName("Should summarize approved product reviews successfully")
    void shouldSummarizeApprovedReviews() {
        Long productId = 42L;
        ProductEntity product = ProductEntity.builder().id(productId).name("Wireless Mouse").build();

        ProductReviewEntity review1 = ProductReviewEntity.builder()
                .id(1L)
                .product(product)
                .rating(5)
                .title("Great ergonomic design")
                .reviewText("Comfortable for all-day coding. Battery life is amazing.")
                .status(ReviewStatus.approved)
                .build();

        ProductReviewEntity review2 = ProductReviewEntity.builder()
                .id(2L)
                .product(product)
                .rating(4)
                .title("Solid mouse")
                .reviewText("Clicks are quiet. Scroll wheel feels a little stiff.")
                .status(ReviewStatus.approved)
                .build();

        when(reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.approved, PageRequest.of(0, 50)))
                .thenReturn(new PageImpl<>(List.of(review1, review2)));

        String jsonResponse = """
                {
                  "positiveThemes": ["Ergonomic design", "Long battery life", "Quiet clicks"],
                  "negativeThemes": ["Stiff scroll wheel"],
                  "recurringComplaints": [],
                  "summaryText": "Customers appreciate the ergonomic comfort and whisper-quiet clicks.",
                  "consensus": "OVERWHELMINGLY_POSITIVE"
                }
                """;

        when(aiClient.completeChat(anyList(), anyInt(), anyDouble()))
                .thenReturn(AiCompletionResponse.builder()
                        .content(jsonResponse)
                        .model("openai/gpt-oss-20b")
                        .promptTokens(180)
                        .completionTokens(80)
                        .totalTokens(260)
                        .latencyMs(210)
                        .build());

        AiReviewSummaryResponse response = service.summarizeProductReviews(productId);

        assertNotNull(response);
        assertEquals(productId, response.getProductId());
        assertEquals("OVERWHELMINGLY_POSITIVE", response.getConsensus());
        assertEquals(2, response.getTotalReviewsAnalyzed());
        assertEquals(4.5, response.getAverageRating());
        assertEquals(1, response.getNegativeThemes().size());
    }

    @Test
    @DisplayName("Should return neutral fallback when product has zero approved reviews")
    void shouldReturnFallbackWhenZeroReviews() {
        Long productId = 42L;

        when(reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.approved, PageRequest.of(0, 50)))
                .thenReturn(new PageImpl<>(List.of()));

        AiReviewSummaryResponse response = service.summarizeProductReviews(productId);

        assertNotNull(response);
        assertEquals(productId, response.getProductId());
        assertEquals(0, response.getTotalReviewsAnalyzed());
        assertEquals("INSUFFICIENT_DATA", response.getConsensus());
        verifyNoInteractions(aiClient);
    }
}
