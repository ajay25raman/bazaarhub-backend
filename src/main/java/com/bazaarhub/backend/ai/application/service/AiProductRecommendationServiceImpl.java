package com.bazaarhub.backend.ai.application.service;

import com.bazaarhub.backend.ai.application.dto.response.AiProductRecommendationResponse;
import com.bazaarhub.backend.ai.infrastructure.client.AiChatMessage;
import com.bazaarhub.backend.ai.infrastructure.client.AiClient;
import com.bazaarhub.backend.ai.infrastructure.client.AiCompletionResponse;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductImageEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductImageRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductVariantRepository;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.review.infrastructure.persistence.repository.SpringDataProductReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AiProductRecommendationServiceImpl implements AiProductRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(AiProductRecommendationServiceImpl.class);

    private final SpringDataProductRepository productRepository;
    private final SpringDataProductVariantRepository variantRepository;
    private final SpringDataInventoryRepository inventoryRepository;
    private final SpringDataProductImageRepository imageRepository;
    private final SpringDataProductReviewRepository reviewRepository;
    private final AiClient aiClient;

    public AiProductRecommendationServiceImpl(SpringDataProductRepository productRepository,
                                              SpringDataProductVariantRepository variantRepository,
                                              SpringDataInventoryRepository inventoryRepository,
                                              SpringDataProductImageRepository imageRepository,
                                              SpringDataProductReviewRepository reviewRepository,
                                              AiClient aiClient) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.inventoryRepository = inventoryRepository;
        this.imageRepository = imageRepository;
        this.reviewRepository = reviewRepository;
        this.aiClient = aiClient;
    }

    @Override
    public List<AiProductRecommendationResponse> getRecommendations(Long categoryId, Long authenticatedUserId, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 30));
        Pageable pageable = PageRequest.of(0, boundedLimit * 2);

        Page<ProductEntity> candidatePage;
        if (categoryId != null) {
            candidatePage = productRepository.findByCategoryIdAndStatusAndApprovalStatus(
                    categoryId, ProductStatus.ACTIVE, ProductApprovalStatus.APPROVED, pageable);
        } else {
            candidatePage = productRepository.findByStatusAndApprovalStatus(
                    ProductStatus.ACTIVE, ProductApprovalStatus.APPROVED, pageable);
        }

        List<ProductEntity> candidates = candidatePage.getContent();
        List<AiProductRecommendationResponse> recommendedItems = new ArrayList<>();

        for (ProductEntity product : candidates) {
            if (recommendedItems.size() >= boundedLimit) {
                break;
            }

            // Check variant and inventory availability
            List<ProductVariantEntity> variants = variantRepository.findByProductId(product.getId());
            boolean hasStock = false;
            BigDecimal minPrice = null;

            for (ProductVariantEntity variant : variants) {
                if (variant.getStatus() == CatalogStatus.ACTIVE) {
                    InventoryEntity inv = inventoryRepository.findByVariantId(variant.getId()).orElse(null);
                    if (inv != null) {
                        int available = inv.getTotalQuantity() - inv.getReservedQuantity();
                        if (available > 0) {
                            hasStock = true;
                            if (minPrice == null || variant.getSellingPrice().compareTo(minPrice) < 0) {
                                minPrice = variant.getSellingPrice();
                            }
                        }
                    }
                }
            }

            // Must have real verified stock availability
            if (!hasStock || minPrice == null) {
                continue;
            }

            // Primary image
            String imageUrl = imageRepository.findByProductIdAndIsPrimaryTrue(product.getId())
                    .map(ProductImageEntity::getImageUrl)
                    .orElseGet(() -> imageRepository.findFirstByProductIdOrderByDisplayOrderAscIdAsc(product.getId())
                            .map(ProductImageEntity::getImageUrl)
                            .orElse(null));

            // Rating
            Double averageRating = 0.0;
            List<Object[]> agg = reviewRepository.getRatingAggregateByProductId(product.getId());
            if (agg != null && !agg.isEmpty() && agg.get(0).length > 1) {
                Number avg = (Number) agg.get(0)[1];
                if (avg != null) {
                    averageRating = Math.round(avg.doubleValue() * 10.0) / 10.0;
                }
            }

            String categoryName = product.getCategory() != null ? product.getCategory().getName() : "Catalog";
            String brandName = product.getBrand() != null ? product.getBrand().getName() : null;

            // Transparent contextual recommendation reason
            String reason = buildContextualReason(categoryName, brandName, averageRating, authenticatedUserId != null);

            recommendedItems.add(AiProductRecommendationResponse.builder()
                    .productId(product.getId())
                    .name(product.getName())
                    .slug(product.getSlug())
                    .categoryName(categoryName)
                    .brandName(brandName)
                    .price(minPrice)
                    .imageUrl(imageUrl)
                    .recommendationReason(reason)
                    .inStock(true)
                    .averageRating(averageRating)
                    .build());
        }

        return recommendedItems;
    }

    private String buildContextualReason(String categoryName, String brandName, Double rating, boolean isAuthenticated) {
        if (rating != null && rating >= 4.5) {
            return String.format("Top-rated in %s with %s★ customer satisfaction and verified availability", categoryName, rating);
        }
        if (brandName != null && !brandName.isBlank()) {
            return String.format("Featured %s selection in %s with ready warehouse stock", brandName, categoryName);
        }
        if (isAuthenticated) {
            return String.format("Popular recommendation in %s with verified real-time stock", categoryName);
        }
        return String.format("Trending marketplace pick in %s with immediate dispatch availability", categoryName);
    }
}
