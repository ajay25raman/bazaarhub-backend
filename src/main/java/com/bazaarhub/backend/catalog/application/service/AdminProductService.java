package com.bazaarhub.backend.catalog.application.service;

import com.bazaarhub.backend.catalog.application.dto.response.ProductDetailResponse;
import com.bazaarhub.backend.catalog.application.dto.response.ProductSummaryResponse;
import com.bazaarhub.backend.catalog.application.mapper.ProductMapper;
import com.bazaarhub.backend.catalog.domain.exception.InvalidProductStateException;
import com.bazaarhub.backend.catalog.domain.exception.ProductNotFoundException;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminProductService {

    private static final Logger log = LoggerFactory.getLogger(AdminProductService.class);

    private final SpringDataProductRepository productRepository;
    private final SpringDataUserRepository userRepository;

    public AdminProductService(SpringDataProductRepository productRepository,
                               SpringDataUserRepository userRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> getProducts(Long sellerId,
                                                    Long categoryId,
                                                    Long brandId,
                                                    ProductStatus status,
                                                    ProductApprovalStatus approvalStatus,
                                                    String keyword,
                                                    Pageable pageable) {
        Specification<ProductEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (sellerId != null) {
                predicates.add(cb.equal(root.get("seller").get("id"), sellerId));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (brandId != null) {
                predicates.add(cb.equal(root.get("brand").get("id"), brandId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (approvalStatus != null) {
                predicates.add(cb.equal(root.get("approvalStatus"), approvalStatus));
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
    public ProductDetailResponse getProductDetail(Long productId) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));
        return ProductMapper.toDetail(product);
    }

    @Transactional
    public ProductDetailResponse approveProduct(Long adminUserId, Long productId) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));

        UserEntity adminUser = userRepository.getReferenceById(adminUserId);

        product.setApprovalStatus(ProductApprovalStatus.APPROVED);
        product.setReviewedBy(adminUser);
        product.setReviewedAt(Instant.now());
        product.setRejectionReason(null);
        if (product.getStatus() == ProductStatus.DRAFT || product.getStatus() == ProductStatus.INACTIVE) {
            product.setStatus(ProductStatus.ACTIVE);
        }
        product.setUpdatedBy(adminUser);

        ProductEntity saved = productRepository.save(product);
        log.info("Admin ID: {} approved product ID: {}", adminUserId, productId);
        return ProductMapper.toDetail(saved);
    }

    @Transactional
    public ProductDetailResponse rejectProduct(Long adminUserId, Long productId, String rejectionReason) {
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new InvalidProductStateException("A rejection reason must be provided when rejecting a product");
        }

        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));

        UserEntity adminUser = userRepository.getReferenceById(adminUserId);

        product.setApprovalStatus(ProductApprovalStatus.REJECTED);
        product.setRejectionReason(rejectionReason.trim());
        product.setReviewedBy(adminUser);
        product.setReviewedAt(Instant.now());
        product.setStatus(ProductStatus.INACTIVE);
        product.setUpdatedBy(adminUser);

        ProductEntity saved = productRepository.save(product);
        log.info("Admin ID: {} rejected product ID: {}, reason: '{}'", adminUserId, productId, rejectionReason);
        return ProductMapper.toDetail(saved);
    }

    @Transactional
    public ProductDetailResponse updateProductStatus(Long adminUserId, Long productId, ProductStatus status) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));

        UserEntity adminUser = userRepository.getReferenceById(adminUserId);

        product.setStatus(status);
        product.setUpdatedBy(adminUser);

        ProductEntity saved = productRepository.save(product);
        log.info("Admin ID: {} updated product ID: {} status to {}", adminUserId, productId, status);
        return ProductMapper.toDetail(saved);
    }
}
