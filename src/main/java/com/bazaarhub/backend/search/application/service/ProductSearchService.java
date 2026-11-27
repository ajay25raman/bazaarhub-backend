package com.bazaarhub.backend.search.application.service;

import com.bazaarhub.backend.catalog.domain.exception.CategoryNotFoundException;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.CategoryEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataCategoryRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductRepository;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.search.application.dto.request.ProductSearchRequest;
import com.bazaarhub.backend.search.application.dto.response.ProductSearchResultDto;
import com.bazaarhub.backend.search.application.mapper.SearchProductMapper;
import com.bazaarhub.backend.search.domain.model.ProductSortOption;
import com.bazaarhub.backend.search.infrastructure.persistence.specification.ProductSearchSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core application service executing storefront product search, faceted filtering,
 * hierarchical category product discovery, and batch stock resolution.
 */
@Service
public class ProductSearchService {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchService.class);

    private final SpringDataProductRepository productRepository;
    private final SpringDataCategoryRepository categoryRepository;
    private final SpringDataInventoryRepository inventoryRepository;

    public ProductSearchService(SpringDataProductRepository productRepository,
                                SpringDataCategoryRepository categoryRepository,
                                SpringDataInventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Executes storefront search with multi-criteria filtering, stock filtering, and pagination.
     */
    @Transactional(readOnly = true)
    public Page<ProductSearchResultDto> searchProducts(ProductSearchRequest request) {
        if (request == null) {
            request = new ProductSearchRequest();
        }
        request.validate();

        // 1. Resolve category hierarchy if categoryId specified
        Set<Long> categoryIds = null;
        if (request.getCategoryId() != null) {
            categoryIds = getActiveCategoryHierarchyIds(request.getCategoryId());
        }

        // 2. Resolve Safe Sort Option
        ProductSortOption defaultSort = (request.getResolvedQuery() != null && !request.getResolvedQuery().isBlank())
                ? ProductSortOption.RELEVANCE
                : ProductSortOption.NEWEST;
        ProductSortOption sortOption = ProductSortOption.fromString(request.getSort(), defaultSort);

        // 3. Build Specification
        Specification<ProductEntity> spec = ProductSearchSpecification.build(request, categoryIds, sortOption);

        int pageNum = request.getPage() != null ? request.getPage() : 0;
        int pageSize = request.getSize() != null ? request.getSize() : 20;
        Pageable pageable = PageRequest.of(pageNum, pageSize);

        log.debug("[SEARCH] Query: '{}', CategoryId: {}, MinPrice: {}, MaxPrice: {}, InStock: {}, Sort: {}",
                request.getResolvedQuery(), request.getCategoryId(), request.getMinPrice(),
                request.getMaxPrice(), request.getInStock(), sortOption);

        // 4. Execute Data Query
        Page<ProductEntity> productPage = productRepository.findAll(spec, pageable);

        // 5. Batch resolve stock availability in a single query
        Set<Long> inStockProductIds = resolveInStockProductIds(productPage.getContent());

        // 6. Map to storefront search DTOs
        return productPage.map(product -> SearchProductMapper.toSearchResult(product, inStockProductIds));
    }

    /**
     * Category-specific discovery endpoint with subcategory support and filtering.
     */
    @Transactional(readOnly = true)
    public Page<ProductSearchResultDto> searchCategoryProducts(Long categoryId, ProductSearchRequest request) {
        if (categoryId == null) {
            throw new CategoryNotFoundException("Category ID cannot be null");
        }
        if (request == null) {
            request = new ProductSearchRequest();
        }
        request.setCategoryId(categoryId);
        return searchProducts(request);
    }

    /**
     * Traverses category tree recursively to collect the specified category ID
     * and all active descendant subcategory IDs.
     */
    public Set<Long> getActiveCategoryHierarchyIds(Long categoryId) {
        CategoryEntity category = categoryRepository.findByIdAndStatus(categoryId, CatalogStatus.ACTIVE)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        Set<Long> categoryIds = new HashSet<>();
        collectActiveCategoryIds(category, categoryIds);
        return categoryIds;
    }

    private void collectActiveCategoryIds(CategoryEntity category, Set<Long> ids) {
        if (category == null || category.getStatus() != CatalogStatus.ACTIVE) {
            return;
        }
        ids.add(category.getId());
        if (category.getChildren() != null) {
            for (CategoryEntity child : category.getChildren()) {
                collectActiveCategoryIds(child, ids);
            }
        }
    }

    /**
     * Batch checks available stock for a list of products in a single database round-trip.
     */
    public Set<Long> resolveInStockProductIds(List<ProductEntity> products) {
        if (products == null || products.isEmpty()) {
            return Collections.emptySet();
        }

        List<Long> variantIds = products.stream()
                .filter(p -> p.getVariants() != null)
                .flatMap(p -> p.getVariants().stream())
                .filter(v -> v.getStatus() == CatalogStatus.ACTIVE)
                .map(ProductVariantEntity::getId)
                .toList();

        if (variantIds.isEmpty()) {
            return Collections.emptySet();
        }

        List<InventoryEntity> inventories = inventoryRepository.findAll((root, query, cb) -> cb.and(
                root.get("variant").get("id").in(variantIds),
                cb.greaterThan(cb.diff(root.get("totalQuantity"), root.get("reservedQuantity")), 0)
        ));

        return inventories.stream()
                .map(inv -> inv.getVariant().getProduct().getId())
                .collect(Collectors.toSet());
    }
}
