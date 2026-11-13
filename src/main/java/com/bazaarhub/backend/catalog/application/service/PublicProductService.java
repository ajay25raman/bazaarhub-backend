package com.bazaarhub.backend.catalog.application.service;

import com.bazaarhub.backend.catalog.application.dto.response.ProductDetailResponse;
import com.bazaarhub.backend.catalog.application.dto.response.ProductSummaryResponse;
import com.bazaarhub.backend.catalog.application.mapper.ProductMapper;
import com.bazaarhub.backend.catalog.domain.exception.ProductNotFoundException;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PublicProductService {

    private final SpringDataProductRepository productRepository;

    public PublicProductService(SpringDataProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> getPublicProducts(Long categoryId,
                                                          Long brandId,
                                                          String keyword,
                                                          Pageable pageable) {
        Specification<ProductEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Strictly filter to active and approved products
            predicates.add(cb.equal(root.get("status"), ProductStatus.ACTIVE));
            predicates.add(cb.equal(root.get("approvalStatus"), ProductApprovalStatus.APPROVED));

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (brandId != null) {
                predicates.add(cb.equal(root.get("brand").get("id"), brandId));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), pattern);
                Predicate slugLike = cb.like(cb.lower(root.get("slug")), pattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(nameLike, slugLike, descLike));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return productRepository.findAll(spec, pageable).map(ProductMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getPublicProductById(Long productId) {
        ProductEntity product = productRepository.findByIdAndStatusAndApprovalStatus(
                productId, ProductStatus.ACTIVE, ProductApprovalStatus.APPROVED
        ).orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));

        return sanitizeForPublic(ProductMapper.toDetail(product));
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getPublicProductBySlug(String slug) {
        ProductEntity product = productRepository.findBySlugAndStatusAndApprovalStatus(
                slug, ProductStatus.ACTIVE, ProductApprovalStatus.APPROVED
        ).orElseThrow(() -> new ProductNotFoundException("Product not found with slug: " + slug));

        return sanitizeForPublic(ProductMapper.toDetail(product));
    }

    /**
     * Sanitizes response for public consumers: removes internal admin review data and inactive variants.
     */
    private ProductDetailResponse sanitizeForPublic(ProductDetailResponse detail) {
        if (detail == null) {
            return null;
        }
        detail.setReviewedBy(null);
        detail.setReviewedAt(null);
        detail.setRejectionReason(null);

        if (detail.getVariants() != null) {
            detail.setVariants(detail.getVariants().stream()
                    .filter(v -> v.getStatus() == CatalogStatus.ACTIVE)
                    .collect(Collectors.toList()));
        }
        return detail;
    }
}
