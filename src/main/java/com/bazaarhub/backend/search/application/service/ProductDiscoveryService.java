package com.bazaarhub.backend.search.application.service;

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
import com.bazaarhub.backend.search.application.mapper.SearchProductMapper;
import com.bazaarhub.backend.search.domain.exception.InvalidSearchFilterException;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Provides rule-based product recommendations and storefront discovery channels:
 * Featured, New Arrivals, Related Products, and Category Recommendations.
 */
@Service
public class ProductDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(ProductDiscoveryService.class);

    private final SpringDataProductRepository productRepository;
    private final SpringDataCategoryRepository categoryRepository;
    private final ProductSearchService productSearchService;

    public ProductDiscoveryService(SpringDataProductRepository productRepository,
                                  SpringDataCategoryRepository categoryRepository,
                                  ProductSearchService productSearchService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productSearchService = productSearchService;
    }

    /**
     * Featured products recommendation.
     * Note: Since MariaDB schema does not have an `is_featured` column,
     * this rule-based implementation surfaces approved, active catalog products
     * with active variants, sorted by creation and update recency.
     */
    @Transactional(readOnly = true)
    public Page<ProductSearchResultDto> getFeaturedProducts(Pageable pageable) {
        Pageable effectivePageable = sanitizePageable(pageable, 10, Sort.by(Sort.Direction.DESC, "updatedAt", "id"));

        Specification<ProductEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = createPublicVisibilityPredicates(root, query, cb);
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ProductEntity> page = productRepository.findAll(spec, effectivePageable);
        Set<Long> inStockIds = productSearchService.resolveInStockProductIds(page.getContent());
        return page.map(p -> SearchProductMapper.toSearchResult(p, inStockIds));
    }

    /**
     * New arrivals discovery channel sorted by creation timestamp descending.
     */
    @Transactional(readOnly = true)
    public Page<ProductSearchResultDto> getNewArrivals(Pageable pageable) {
        Pageable effectivePageable = sanitizePageable(pageable, 10, Sort.by(Sort.Direction.DESC, "createdAt", "id"));

        Specification<ProductEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = createPublicVisibilityPredicates(root, query, cb);
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ProductEntity> page = productRepository.findAll(spec, effectivePageable);
        Set<Long> inStockIds = productSearchService.resolveInStockProductIds(page.getContent());
        return page.map(p -> SearchProductMapper.toSearchResult(p, inStockIds));
    }

    /**
     * Related products recommendation based on shared category (or shared brand),
     * strictly excluding the target product.
     */
    @Transactional(readOnly = true)
    public Page<ProductSearchResultDto> getRelatedProducts(Long productId, Pageable pageable) {
        if (productId == null) {
            throw new ProductNotFoundException("Product ID cannot be null");
        }

        ProductEntity targetProduct = productRepository.findByIdAndStatusAndApprovalStatus(
                productId, ProductStatus.ACTIVE, ProductApprovalStatus.APPROVED
        ).orElseThrow(() -> new ProductNotFoundException(productId));

        Pageable effectivePageable = sanitizePageable(pageable, 10, Sort.by(Sort.Direction.DESC, "createdAt", "id"));

        Long categoryId = targetProduct.getCategory() != null ? targetProduct.getCategory().getId() : null;
        Long brandId = targetProduct.getBrand() != null ? targetProduct.getBrand().getId() : null;

        Specification<ProductEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = createPublicVisibilityPredicates(root, query, cb);

            // Exclude current product
            predicates.add(cb.notEqual(root.get("id"), productId));

            // Match same category or brand
            List<Predicate> relationPredicates = new ArrayList<>();
            if (categoryId != null) {
                relationPredicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (brandId != null) {
                relationPredicates.add(cb.equal(root.get("brand").get("id"), brandId));
            }

            if (!relationPredicates.isEmpty()) {
                predicates.add(cb.or(relationPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ProductEntity> page = productRepository.findAll(spec, effectivePageable);
        Set<Long> inStockIds = productSearchService.resolveInStockProductIds(page.getContent());
        return page.map(p -> SearchProductMapper.toSearchResult(p, inStockIds));
    }

    /**
     * Category recommendations endpoint returning active catalog products in category and active subcategories.
     */
    @Transactional(readOnly = true)
    public Page<ProductSearchResultDto> getProductsByCategory(Long categoryId, Pageable pageable) {
        if (categoryId == null) {
            throw new CategoryNotFoundException("Category ID cannot be null");
        }

        // Validate category exists and is active
        CategoryEntity category = categoryRepository.findByIdAndStatus(categoryId, CatalogStatus.ACTIVE)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        Set<Long> categoryIds = productSearchService.getActiveCategoryHierarchyIds(categoryId);
        Pageable effectivePageable = sanitizePageable(pageable, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id"));

        Specification<ProductEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = createPublicVisibilityPredicates(root, query, cb);
            predicates.add(root.get("category").get("id").in(categoryIds));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ProductEntity> page = productRepository.findAll(spec, effectivePageable);
        Set<Long> inStockIds = productSearchService.resolveInStockProductIds(page.getContent());
        return page.map(p -> SearchProductMapper.toSearchResult(p, inStockIds));
    }

    private List<Predicate> createPublicVisibilityPredicates(Root<ProductEntity> root,
                                                             jakarta.persistence.criteria.CriteriaQuery<?> query,
                                                             jakarta.persistence.criteria.CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("status"), ProductStatus.ACTIVE));
        predicates.add(cb.equal(root.get("approvalStatus"), ProductApprovalStatus.APPROVED));
        predicates.add(cb.equal(root.get("seller").get("approvalStatus"), SellerApprovalStatus.APPROVED));

        // Must have at least one active variant
        Subquery<Long> vSub = query.subquery(Long.class);
        Root<ProductVariantEntity> vRoot = vSub.from(ProductVariantEntity.class);
        vSub.select(vRoot.get("id"))
                .where(
                        cb.equal(vRoot.get("product"), root),
                        cb.equal(vRoot.get("status"), CatalogStatus.ACTIVE)
                );
        predicates.add(cb.exists(vSub));

        return predicates;
    }

    private Pageable sanitizePageable(Pageable pageable, int defaultSize, Sort defaultSort) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, defaultSize, defaultSort);
        }
        if (pageable.getPageSize() > 100) {
            throw new InvalidSearchFilterException("Page size cannot exceed 100");
        }
        if (pageable.getPageNumber() < 0) {
            throw new InvalidSearchFilterException("Page index must not be less than zero");
        }

        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : defaultSort;
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
