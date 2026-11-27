package com.bazaarhub.backend.search.infrastructure.persistence.specification;

import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.bazaarhub.backend.search.application.dto.request.ProductSearchRequest;
import com.bazaarhub.backend.search.domain.model.ProductSortOption;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Dynamic JPA Criteria specification for storefront product search, category browsing,
 * multi-attribute filtering, stock availability, and safe allowlisted sorting.
 */
public class ProductSearchSpecification {

    private ProductSearchSpecification() {
    }

    public static Specification<ProductEntity> build(ProductSearchRequest request,
                                                     Collection<Long> categoryIds,
                                                     ProductSortOption sortOption) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory Public Visibility Constraints
            predicates.add(cb.equal(root.get("status"), ProductStatus.ACTIVE));
            predicates.add(cb.equal(root.get("approvalStatus"), ProductApprovalStatus.APPROVED));
            predicates.add(cb.equal(root.get("seller").get("approvalStatus"), SellerApprovalStatus.APPROVED));

            // Must have at least one active variant configured
            Subquery<Long> activeVariantSubquery = query.subquery(Long.class);
            Root<ProductVariantEntity> activeVariantRoot = activeVariantSubquery.from(ProductVariantEntity.class);
            activeVariantSubquery.select(activeVariantRoot.get("id"))
                    .where(
                            cb.equal(activeVariantRoot.get("product"), root),
                            cb.equal(activeVariantRoot.get("status"), CatalogStatus.ACTIVE)
                    );
            predicates.add(cb.exists(activeVariantSubquery));

            // 2. Category & Subcategory Filter
            if (request != null && request.getSubcategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), request.getSubcategoryId()));
            } else if (categoryIds != null && !categoryIds.isEmpty()) {
                predicates.add(root.get("category").get("id").in(categoryIds));
            } else if (request != null && request.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), request.getCategoryId()));
            }

            // 3. Brand Filter
            if (request != null && request.getBrandId() != null) {
                predicates.add(cb.equal(root.get("brand").get("id"), request.getBrandId()));
            }

            // 4. Seller Filter
            if (request != null && request.getSellerId() != null) {
                predicates.add(cb.equal(root.get("seller").get("id"), request.getSellerId()));
            }

            // 5. Keyword Search (matches product name, description, and variant SKU)
            if (request != null) {
                String searchTerms = request.getResolvedQuery();
                if (searchTerms != null && !searchTerms.isBlank()) {
                    String cleanQuery = searchTerms.trim();
                    String[] tokens = cleanQuery.split("\\s+");

                    List<Predicate> tokenPredicates = new ArrayList<>();
                    for (String token : tokens) {
                        if (token.isBlank()) continue;
                        String pattern = "%" + token.toLowerCase() + "%";

                        Predicate nameLike = cb.like(cb.lower(root.get("name")), pattern);
                        Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);

                        // Variant SKU match subquery
                        Subquery<Long> skuSubquery = query.subquery(Long.class);
                        Root<ProductVariantEntity> skuVariantRoot = skuSubquery.from(ProductVariantEntity.class);
                        skuSubquery.select(skuVariantRoot.get("id"))
                                .where(
                                        cb.equal(skuVariantRoot.get("product"), root),
                                        cb.equal(skuVariantRoot.get("status"), CatalogStatus.ACTIVE),
                                        cb.like(cb.lower(skuVariantRoot.get("sku")), pattern)
                                );
                        Predicate skuLike = cb.exists(skuSubquery);

                        tokenPredicates.add(cb.or(nameLike, descLike, skuLike));
                    }

                    if (!tokenPredicates.isEmpty()) {
                        // All tokens must match for multi-word queries
                        predicates.add(cb.and(tokenPredicates.toArray(new Predicate[0])));
                    }
                }
            }

            // 6. Price Range Filter (against active variants)
            if (request != null && (request.getMinPrice() != null || request.getMaxPrice() != null)) {
                Subquery<Long> priceSubquery = query.subquery(Long.class);
                Root<ProductVariantEntity> pvRoot = priceSubquery.from(ProductVariantEntity.class);
                priceSubquery.select(pvRoot.get("id"));

                List<Predicate> pvPredicates = new ArrayList<>();
                pvPredicates.add(cb.equal(pvRoot.get("product"), root));
                pvPredicates.add(cb.equal(pvRoot.get("status"), CatalogStatus.ACTIVE));

                if (request.getMinPrice() != null) {
                    pvPredicates.add(cb.greaterThanOrEqualTo(pvRoot.get("sellingPrice"), request.getMinPrice()));
                }
                if (request.getMaxPrice() != null) {
                    pvPredicates.add(cb.lessThanOrEqualTo(pvRoot.get("sellingPrice"), request.getMaxPrice()));
                }

                priceSubquery.where(pvPredicates.toArray(new Predicate[0]));
                predicates.add(cb.exists(priceSubquery));
            }

            // 7. Stock Availability Filter
            if (request != null && request.getInStock() != null) {
                Subquery<Long> stockSubquery = query.subquery(Long.class);
                Root<InventoryEntity> invRoot = stockSubquery.from(InventoryEntity.class);
                stockSubquery.select(invRoot.get("id"));

                Predicate invProdMatch = cb.equal(invRoot.get("variant").get("product"), root);
                Predicate invVariantActive = cb.equal(invRoot.get("variant").get("status"), CatalogStatus.ACTIVE);
                Predicate qtyAvailable = cb.greaterThan(
                        cb.diff(invRoot.get("totalQuantity"), invRoot.get("reservedQuantity")),
                        0
                );

                stockSubquery.where(invProdMatch, invVariantActive, qtyAvailable);

                if (Boolean.TRUE.equals(request.getInStock())) {
                    predicates.add(cb.exists(stockSubquery));
                } else {
                    predicates.add(cb.not(cb.exists(stockSubquery)));
                }
            }

            // 8. Sorting Configuration (only applied to data queries, omitted for count queries)
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                applySorting(root, query, cb, sortOption, request != null ? request.getResolvedQuery() : null);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void applySorting(Root<ProductEntity> root,
                                     CriteriaQuery<?> query,
                                     CriteriaBuilder cb,
                                     ProductSortOption sortOption,
                                     String searchQuery) {
        ProductSortOption option = sortOption != null ? sortOption : ProductSortOption.NEWEST;

        switch (option) {
            case PRICE_ASC -> {
                Subquery<BigDecimal> minPriceSubquery = query.subquery(BigDecimal.class);
                Root<ProductVariantEntity> pvRoot = minPriceSubquery.from(ProductVariantEntity.class);
                minPriceSubquery.select(cb.min(pvRoot.get("sellingPrice")))
                        .where(
                                cb.equal(pvRoot.get("product"), root),
                                cb.equal(pvRoot.get("status"), CatalogStatus.ACTIVE)
                        );
                query.orderBy(cb.asc(minPriceSubquery), cb.asc(root.get("id")));
            }
            case PRICE_DESC -> {
                Subquery<BigDecimal> maxPriceSubquery = query.subquery(BigDecimal.class);
                Root<ProductVariantEntity> pvRoot = maxPriceSubquery.from(ProductVariantEntity.class);
                maxPriceSubquery.select(cb.max(pvRoot.get("sellingPrice")))
                        .where(
                                cb.equal(pvRoot.get("product"), root),
                                cb.equal(pvRoot.get("status"), CatalogStatus.ACTIVE)
                        );
                query.orderBy(cb.desc(maxPriceSubquery), cb.desc(root.get("id")));
            }
            case NAME_ASC -> query.orderBy(cb.asc(cb.lower(root.get("name"))), cb.asc(root.get("id")));
            case NAME_DESC -> query.orderBy(cb.desc(cb.lower(root.get("name"))), cb.desc(root.get("id")));
            case RELEVANCE -> {
                if (searchQuery != null && !searchQuery.isBlank()) {
                    String clean = searchQuery.trim().toLowerCase();
                    // Prioritize exact name match, then prefix match, then fallback to newest
                    Expression<Integer> exactMatch = cb.<Integer>selectCase()
                            .when(cb.equal(cb.lower(root.get("name")), clean), 1)
                            .when(cb.like(cb.lower(root.get("name")), clean + "%"), 2)
                            .otherwise(3);
                    query.orderBy(cb.asc(exactMatch), cb.desc(root.get("createdAt")), cb.desc(root.get("id")));
                } else {
                    query.orderBy(cb.desc(root.get("createdAt")), cb.desc(root.get("id")));
                }
            }
            case NEWEST -> query.orderBy(cb.desc(root.get("createdAt")), cb.desc(root.get("id")));
        }
    }
}
