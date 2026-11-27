package com.bazaarhub.backend.search;

import com.bazaarhub.backend.catalog.domain.exception.CategoryNotFoundException;
import com.bazaarhub.backend.catalog.domain.exception.ProductNotFoundException;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.CategoryEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataCategoryRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductRepository;
import com.bazaarhub.backend.search.application.dto.response.ProductSearchResultDto;
import com.bazaarhub.backend.search.application.service.ProductDiscoveryService;
import com.bazaarhub.backend.search.application.service.ProductSearchService;
import com.bazaarhub.backend.search.domain.exception.InvalidSearchFilterException;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductDiscoveryServiceUnitTest {

    @Mock
    private SpringDataProductRepository productRepository;

    @Mock
    private SpringDataCategoryRepository categoryRepository;

    @Mock
    private ProductSearchService productSearchService;

    private ProductDiscoveryService productDiscoveryService;

    @BeforeEach
    void setUp() {
        productDiscoveryService = new ProductDiscoveryService(productRepository, categoryRepository, productSearchService);
    }

    @Test
    @DisplayName("getFeaturedProducts: should return paginated featured products")
    void getFeaturedProducts_Success() {
        ProductEntity product = createSampleProduct(1L, "Featured Gadget");
        Page<ProductEntity> page = new PageImpl<>(List.of(product));

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productSearchService.resolveInStockProductIds(any())).thenReturn(Set.of(1L));

        Page<ProductSearchResultDto> result = productDiscoveryService.getFeaturedProducts(PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Featured Gadget");
        assertThat(result.getContent().get(0).isInStock()).isTrue();
    }

    @Test
    @DisplayName("getNewArrivals: should return paginated new arrivals")
    void getNewArrivals_Success() {
        ProductEntity product = createSampleProduct(2L, "New Arrival Device");
        Page<ProductEntity> page = new PageImpl<>(List.of(product));

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productSearchService.resolveInStockProductIds(any())).thenReturn(Collections.emptySet());

        Page<ProductSearchResultDto> result = productDiscoveryService.getNewArrivals(PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("New Arrival Device");
        assertThat(result.getContent().get(0).isInStock()).isFalse();
    }

    @Test
    @DisplayName("getRelatedProducts: should return related products from same category")
    void getRelatedProducts_ValidProduct_Success() {
        ProductEntity targetProduct = createSampleProduct(10L, "Target Product");
        ProductEntity relatedProduct = createSampleProduct(11L, "Related Product");

        when(productRepository.findByIdAndStatusAndApprovalStatus(10L, ProductStatus.ACTIVE, ProductApprovalStatus.APPROVED))
                .thenReturn(Optional.of(targetProduct));

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(relatedProduct)));
        when(productSearchService.resolveInStockProductIds(any())).thenReturn(Set.of(11L));

        Page<ProductSearchResultDto> result = productDiscoveryService.getRelatedProducts(10L, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(11L);
    }

    @Test
    @DisplayName("getRelatedProducts: should throw ProductNotFoundException when target product not found")
    void getRelatedProducts_ProductNotFound_ThrowsException() {
        when(productRepository.findByIdAndStatusAndApprovalStatus(999L, ProductStatus.ACTIVE, ProductApprovalStatus.APPROVED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productDiscoveryService.getRelatedProducts(999L, PageRequest.of(0, 10)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("getProductsByCategory: should return products for active category")
    void getProductsByCategory_ValidCategory_Success() {
        CategoryEntity category = CategoryEntity.builder()
                .id(5L)
                .name("Books")
                .status(CatalogStatus.ACTIVE)
                .build();

        ProductEntity product = createSampleProduct(20L, "Java 21 In Action");

        when(categoryRepository.findByIdAndStatus(5L, CatalogStatus.ACTIVE)).thenReturn(Optional.of(category));
        when(productSearchService.getActiveCategoryHierarchyIds(5L)).thenReturn(Set.of(5L));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));
        when(productSearchService.resolveInStockProductIds(any())).thenReturn(Set.of(20L));

        Page<ProductSearchResultDto> result = productDiscoveryService.getProductsByCategory(5L, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Java 21 In Action");
    }

    @Test
    @DisplayName("getProductsByCategory: should throw CategoryNotFoundException when category not found")
    void getProductsByCategory_CategoryNotFound_ThrowsException() {
        when(categoryRepository.findByIdAndStatus(999L, CatalogStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productDiscoveryService.getProductsByCategory(999L, PageRequest.of(0, 10)))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    @DisplayName("discovery endpoints: should throw InvalidSearchFilterException when page size exceeds 100")
    void discoveryEndpoints_PageSizeExceedsLimit_ThrowsException() {
        Pageable invalidPageable = PageRequest.of(0, 150);

        assertThatThrownBy(() -> productDiscoveryService.getFeaturedProducts(invalidPageable))
                .isInstanceOf(InvalidSearchFilterException.class)
                .hasMessageContaining("Page size cannot exceed 100");
    }

    private ProductEntity createSampleProduct(Long id, String name) {
        SellerEntity seller = SellerEntity.builder()
                .id(1L)
                .businessName("Bazaar Electronics")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .build();

        CategoryEntity category = CategoryEntity.builder()
                .id(5L)
                .name("General")
                .slug("general")
                .status(CatalogStatus.ACTIVE)
                .build();

        ProductEntity product = ProductEntity.builder()
                .id(id)
                .name(name)
                .slug("slug-" + id)
                .description("Description of " + name)
                .seller(seller)
                .category(category)
                .status(ProductStatus.ACTIVE)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ProductVariantEntity variant = ProductVariantEntity.builder()
                .id(id * 10)
                .product(product)
                .sku("SKU-" + id)
                .variantName("Default Variant")
                .sellingPrice(new BigDecimal("299.00"))
                .mrp(new BigDecimal("350.00"))
                .status(CatalogStatus.ACTIVE)
                .build();

        product.setVariants(List.of(variant));
        product.setImages(Collections.emptyList());
        return product;
    }
}
