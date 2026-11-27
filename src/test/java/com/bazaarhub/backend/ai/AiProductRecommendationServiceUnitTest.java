package com.bazaarhub.backend.ai;

import com.bazaarhub.backend.ai.application.dto.response.AiProductRecommendationResponse;
import com.bazaarhub.backend.ai.application.service.AiProductRecommendationServiceImpl;
import com.bazaarhub.backend.ai.infrastructure.client.AiClient;
import com.bazaarhub.backend.ai.infrastructure.client.AiCompletionResponse;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.CategoryEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductImageRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductVariantRepository;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.review.infrastructure.persistence.repository.SpringDataProductReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiProductRecommendationServiceUnitTest {

    @Mock
    private SpringDataProductRepository productRepository;

    @Mock
    private SpringDataProductVariantRepository variantRepository;

    @Mock
    private SpringDataInventoryRepository inventoryRepository;

    @Mock
    private SpringDataProductImageRepository imageRepository;

    @Mock
    private SpringDataProductReviewRepository reviewRepository;

    @Mock
    private AiClient aiClient;

    @InjectMocks
    private AiProductRecommendationServiceImpl service;

    private ProductEntity product1;
    private ProductVariantEntity variant1;

    @BeforeEach
    void setUp() {
        lenient().when(aiClient.getProviderName()).thenReturn("Groq");
        lenient().when(aiClient.getModelName()).thenReturn("openai/gpt-oss-20b");

        CategoryEntity category = CategoryEntity.builder().id(5L).name("Electronics").build();

        product1 = ProductEntity.builder()
                .id(101L)
                .name("Noise Cancelling Headphones")
                .slug("noise-cancelling-headphones")
                .status(ProductStatus.ACTIVE)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .category(category)
                .build();

        variant1 = ProductVariantEntity.builder()
                .id(201L)
                .product(product1)
                .sku("NCH-BLK-01")
                .sellingPrice(new BigDecimal("199.99"))
                .build();
    }

    @Test
    @DisplayName("Should return database-backed recommendations with AI rationale")
    void shouldReturnRecommendationsWithAiRationale() {
        when(productRepository.findByStatusAndApprovalStatus(eq(ProductStatus.ACTIVE), eq(ProductApprovalStatus.APPROVED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product1)));

        when(variantRepository.findByProductId(101L)).thenReturn(List.of(variant1));

        InventoryEntity inventory = InventoryEntity.builder()
                .id(1L)
                .variant(variant1)
                .totalQuantity(25)
                .reservedQuantity(5)
                .build();

        when(inventoryRepository.findByVariantId(variant1.getId())).thenReturn(Optional.of(inventory));

        List<AiProductRecommendationResponse> recommendations = service.getRecommendations(null, 10L, 5);

        assertNotNull(recommendations);
        assertEquals(1, recommendations.size());
        assertEquals(101L, recommendations.get(0).getProductId());
        assertEquals("Noise Cancelling Headphones", recommendations.get(0).getName());
        assertTrue(recommendations.get(0).isInStock());
    }

    @Test
    @DisplayName("Should filter out out-of-stock products from recommendations")
    void shouldFilterOutOutOfStockProducts() {
        when(productRepository.findByStatusAndApprovalStatus(eq(ProductStatus.ACTIVE), eq(ProductApprovalStatus.APPROVED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product1)));

        when(variantRepository.findByProductId(101L)).thenReturn(List.of(variant1));

        InventoryEntity zeroInventory = InventoryEntity.builder()
                .id(1L)
                .variant(variant1)
                .totalQuantity(5)
                .reservedQuantity(5) // 5 - 5 = 0
                .build();

        when(inventoryRepository.findByVariantId(variant1.getId())).thenReturn(Optional.of(zeroInventory));

        List<AiProductRecommendationResponse> recommendations = service.getRecommendations(null, 10L, 5);

        assertNotNull(recommendations);
        assertTrue(recommendations.isEmpty(), "Zero stock products must not be recommended");
        verifyNoInteractions(aiClient);
    }
}
