package com.bazaarhub.backend.search;

import com.bazaarhub.backend.catalog.domain.exception.CategoryNotFoundException;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.CategoryEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataCategoryRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductRepository;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.search.application.dto.request.ProductSearchRequest;
import com.bazaarhub.backend.search.application.dto.response.ProductSearchResultDto;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceUnitTest {

    @Mock
    private SpringDataProductRepository productRepository;

    @Mock
    private SpringDataCategoryRepository categoryRepository;

    @Mock
    private SpringDataInventoryRepository inventoryRepository;

    private ProductSearchService productSearchService;

    @BeforeEach
    void setUp() {
        productSearchService = new ProductSearchService(productRepository, categoryRepository, inventoryRepository);
    }

    @Test
    @DisplayName("searchProducts: should execute search successfully with default parameters")
    void searchProducts_DefaultParams_Success() {
        ProductEntity product = createSampleProduct(1L, "Laptop", "laptop-slug");
        Page<ProductEntity> productPage = new PageImpl<>(List.of(product));

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(productPage);

        Page<ProductSearchResultDto> result = productSearchService.searchProducts(new ProductSearchRequest());

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Laptop");
        verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("searchProducts: should throw InvalidSearchFilterException when minPrice > maxPrice")
    void searchProducts_MinPriceGreaterThanMaxPrice_ThrowsException() {
        ProductSearchRequest request = ProductSearchRequest.builder()
                .minPrice(new BigDecimal("500.00"))
                .maxPrice(new BigDecimal("100.00"))
                .build();

        assertThatThrownBy(() -> productSearchService.searchProducts(request))
                .isInstanceOf(InvalidSearchFilterException.class)
                .hasMessageContaining("Minimum price cannot be greater than maximum price");
    }

    @Test
    @DisplayName("searchProducts: should throw InvalidSearchFilterException when minPrice is negative")
    void searchProducts_NegativeMinPrice_ThrowsException() {
        ProductSearchRequest request = ProductSearchRequest.builder()
                .minPrice(new BigDecimal("-10.00"))
                .build();

        assertThatThrownBy(() -> productSearchService.searchProducts(request))
                .isInstanceOf(InvalidSearchFilterException.class)
                .hasMessageContaining("Minimum price cannot be negative");
    }

    @Test
    @DisplayName("searchProducts: should throw InvalidSearchFilterException when maxPrice is negative")
    void searchProducts_NegativeMaxPrice_ThrowsException() {
        ProductSearchRequest request = ProductSearchRequest.builder()
                .maxPrice(new BigDecimal("-1.00"))
                .build();

        assertThatThrownBy(() -> productSearchService.searchProducts(request))
                .isInstanceOf(InvalidSearchFilterException.class)
                .hasMessageContaining("Maximum price cannot be negative");
    }

    @Test
    @DisplayName("searchProducts: should throw InvalidSearchFilterException when page size exceeds 100")
    void searchProducts_PageSizeExceedsLimit_ThrowsException() {
        ProductSearchRequest request = ProductSearchRequest.builder()
                .size(150)
                .build();

        assertThatThrownBy(() -> productSearchService.searchProducts(request))
                .isInstanceOf(InvalidSearchFilterException.class)
                .hasMessageContaining("Page size cannot exceed 100");
    }

    @Test
    @DisplayName("searchProducts: should throw InvalidSearchFilterException when page index is negative")
    void searchProducts_NegativePageIndex_ThrowsException() {
        ProductSearchRequest request = ProductSearchRequest.builder()
                .page(-1)
                .build();

        assertThatThrownBy(() -> productSearchService.searchProducts(request))
                .isInstanceOf(InvalidSearchFilterException.class)
                .hasMessageContaining("Page index must not be less than zero");
    }

    @Test
    @DisplayName("searchCategoryProducts: should resolve category hierarchy and execute search")
    void searchCategoryProducts_ValidCategory_Success() {
        CategoryEntity parentCat = CategoryEntity.builder()
                .id(10L)
                .name("Electronics")
                .slug("electronics")
                .status(CatalogStatus.ACTIVE)
                .build();

        CategoryEntity childCat = CategoryEntity.builder()
                .id(11L)
                .name("Laptops")
                .slug("laptops")
                .parent(parentCat)
                .status(CatalogStatus.ACTIVE)
                .build();
        parentCat.setChildren(List.of(childCat));

        when(categoryRepository.findByIdAndStatus(10L, CatalogStatus.ACTIVE)).thenReturn(Optional.of(parentCat));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        Page<ProductSearchResultDto> result = productSearchService.searchCategoryProducts(10L, new ProductSearchRequest());

        assertThat(result).isNotNull();
        verify(categoryRepository).findByIdAndStatus(10L, CatalogStatus.ACTIVE);
        verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("searchCategoryProducts: should throw CategoryNotFoundException when category not found or inactive")
    void searchCategoryProducts_CategoryNotFound_ThrowsException() {
        when(categoryRepository.findByIdAndStatus(999L, CatalogStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productSearchService.searchCategoryProducts(999L, new ProductSearchRequest()))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    @DisplayName("resolveInStockProductIds: should identify in-stock products in single query")
    void resolveInStockProductIds_CorrectlyIdentifiesInStock() {
        ProductEntity product1 = createSampleProduct(1L, "P1", "p1");
        ProductEntity product2 = createSampleProduct(2L, "P2", "p2");

        ProductVariantEntity v1 = product1.getVariants().get(0);
        InventoryEntity inv1 = InventoryEntity.builder()
                .id(100L)
                .variant(v1)
                .totalQuantity(50)
                .reservedQuantity(5)
                .build();

        when(inventoryRepository.findAll(any(Specification.class))).thenReturn(List.of(inv1));

        Set<Long> inStockIds = productSearchService.resolveInStockProductIds(List.of(product1, product2));

        assertThat(inStockIds).containsExactly(1L);
    }

    private ProductEntity createSampleProduct(Long id, String name, String slug) {
        SellerEntity seller = SellerEntity.builder()
                .id(1L)
                .businessName("Tech Seller")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .build();

        CategoryEntity category = CategoryEntity.builder()
                .id(10L)
                .name("Computers")
                .slug("computers")
                .status(CatalogStatus.ACTIVE)
                .build();

        ProductEntity product = ProductEntity.builder()
                .id(id)
                .name(name)
                .slug(slug)
                .description("Detailed description")
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
                .sellingPrice(new BigDecimal("999.00"))
                .mrp(new BigDecimal("1200.00"))
                .status(CatalogStatus.ACTIVE)
                .build();

        product.setVariants(List.of(variant));
        product.setImages(Collections.emptyList());
        return product;
    }
}
