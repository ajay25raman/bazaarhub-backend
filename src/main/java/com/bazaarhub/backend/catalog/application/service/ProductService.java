package com.bazaarhub.backend.catalog.application.service;

import com.bazaarhub.backend.catalog.application.dto.request.ProductCreateRequest;
import com.bazaarhub.backend.catalog.application.dto.request.ProductUpdateRequest;
import com.bazaarhub.backend.catalog.application.dto.response.ProductDetailResponse;
import com.bazaarhub.backend.catalog.application.dto.response.ProductSummaryResponse;
import com.bazaarhub.backend.catalog.application.mapper.ProductMapper;
import com.bazaarhub.backend.catalog.domain.exception.*;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.BrandEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.CategoryEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataBrandRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataCategoryRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import com.bazaarhub.backend.shared.application.util.SlugUtils;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final SpringDataProductRepository productRepository;
    private final SpringDataCategoryRepository categoryRepository;
    private final SpringDataBrandRepository brandRepository;
    private final SpringDataSellerRepository sellerRepository;
    private final SpringDataUserRepository userRepository;

    public ProductService(SpringDataProductRepository productRepository,
                          SpringDataCategoryRepository categoryRepository,
                          SpringDataBrandRepository brandRepository,
                          SpringDataSellerRepository sellerRepository,
                          SpringDataUserRepository userRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.sellerRepository = sellerRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProductDetailResponse createProduct(Long userId, ProductCreateRequest request) {
        // 1. Verify seller existence and approved status
        SellerEntity seller = resolveApprovedSeller(userId);

        // 2. Validate Category
        CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with ID: " + request.getCategoryId()));
        if (category.getStatus() != CatalogStatus.ACTIVE) {
            throw new InvalidProductStateException("Cannot assign product to inactive category: " + category.getName());
        }

        // 3. Validate Brand (optional)
        BrandEntity brand = null;
        if (request.getBrandId() != null) {
            brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new BrandNotFoundException("Brand not found with ID: " + request.getBrandId()));
            if (brand.getStatus() != CatalogStatus.ACTIVE) {
                throw new InvalidProductStateException("Cannot assign product to inactive brand: " + brand.getName());
            }
        }

        // 4. Resolve and validate Slug
        String rawSlug = request.getSlug() != null && !request.getSlug().isBlank()
                ? SlugUtils.toSlug(request.getSlug())
                : SlugUtils.toSlug(request.getName());

        if (rawSlug.isBlank()) {
            rawSlug = "prod-" + System.currentTimeMillis();
        }

        if (productRepository.existsBySlug(rawSlug)) {
            throw new ProductAlreadyExistsException("Product with slug '" + rawSlug + "' already exists");
        }

        // 5. Initial status: DRAFT or ACTIVE (default DRAFT). Moderation approval status is always PENDING.
        ProductStatus initialStatus = request.getStatus() != null ? request.getStatus() : ProductStatus.DRAFT;
        if (initialStatus == ProductStatus.ARCHIVED) {
            throw new InvalidProductStateException("Cannot create a product directly in ARCHIVED status");
        }

        UserEntity user = userRepository.getReferenceById(userId);

        ProductEntity product = ProductEntity.builder()
                .seller(seller)
                .category(category)
                .brand(brand)
                .name(request.getName().trim())
                .slug(rawSlug)
                .description(request.getDescription())
                .status(initialStatus)
                .approvalStatus(ProductApprovalStatus.PENDING)
                .createdBy(user)
                .updatedBy(user)
                .build();

        ProductEntity saved = productRepository.save(product);
        log.info("Created product ID: {} with slug: '{}' for seller ID: {}", saved.getId(), saved.getSlug(), seller.getId());
        return ProductMapper.toDetail(saved);
    }

    @Transactional
    public ProductDetailResponse updateProduct(Long userId, Long productId, ProductUpdateRequest request) {
        ProductEntity product = getProductAndValidateOwnership(productId, userId);

        // 1. Validate Category
        if (!product.getCategory().getId().equals(request.getCategoryId())) {
            CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new CategoryNotFoundException("Category not found with ID: " + request.getCategoryId()));
            if (category.getStatus() != CatalogStatus.ACTIVE) {
                throw new InvalidProductStateException("Cannot assign product to inactive category: " + category.getName());
            }
            product.setCategory(category);
        }

        // 2. Validate Brand
        if (request.getBrandId() != null) {
            if (product.getBrand() == null || !product.getBrand().getId().equals(request.getBrandId())) {
                BrandEntity brand = brandRepository.findById(request.getBrandId())
                        .orElseThrow(() -> new BrandNotFoundException("Brand not found with ID: " + request.getBrandId()));
                if (brand.getStatus() != CatalogStatus.ACTIVE) {
                    throw new InvalidProductStateException("Cannot assign product to inactive brand: " + brand.getName());
                }
                product.setBrand(brand);
            }
        } else {
            product.setBrand(null);
        }

        // 3. Validate Slug if provided
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            String newSlug = SlugUtils.toSlug(request.getSlug());
            if (!newSlug.equalsIgnoreCase(product.getSlug())) {
                if (productRepository.existsBySlugAndIdNot(newSlug, productId)) {
                    throw new ProductAlreadyExistsException("Product slug '" + newSlug + "' is already in use by another product");
                }
                product.setSlug(newSlug);
            }
        }

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setUpdatedBy(userRepository.getReferenceById(userId));

        ProductEntity saved = productRepository.save(product);
        log.info("Updated product ID: {} by seller user ID: {}", saved.getId(), userId);
        return ProductMapper.toDetail(saved);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getSellerProduct(Long userId, Long productId) {
        ProductEntity product = getProductAndValidateOwnership(productId, userId);
        return ProductMapper.toDetail(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> getSellerProducts(Long userId,
                                                          ProductStatus status,
                                                          ProductApprovalStatus approvalStatus,
                                                          String keyword,
                                                          Pageable pageable) {
        SellerEntity seller = resolveSeller(userId);

        Specification<ProductEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("seller").get("id"), seller.getId()));

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

    @Transactional
    public ProductDetailResponse updateProductStatus(Long userId, Long productId, ProductStatus newStatus) {
        ProductEntity product = getProductAndValidateOwnership(productId, userId);

        if (newStatus == ProductStatus.ACTIVE && product.getApprovalStatus() == ProductApprovalStatus.REJECTED) {
            throw new InvalidProductStateException("Cannot activate a rejected product. Please edit details and request re-approval.");
        }

        product.setStatus(newStatus);
        product.setUpdatedBy(userRepository.getReferenceById(userId));

        ProductEntity saved = productRepository.save(product);
        log.info("Updated product ID: {} status to {} by seller user ID: {}", saved.getId(), newStatus, userId);
        return ProductMapper.toDetail(saved);
    }

    @Transactional
    public void deleteProduct(Long userId, Long productId) {
        ProductEntity product = getProductAndValidateOwnership(productId, userId);
        // Soft delete / archive to ensure referential integrity
        product.setStatus(ProductStatus.ARCHIVED);
        product.setUpdatedBy(userRepository.getReferenceById(userId));
        productRepository.save(product);
        log.info("Archived product ID: {} by seller user ID: {}", productId, userId);
    }

    public ProductEntity getProductAndValidateOwnership(Long productId, Long userId) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));

        if (product.getSeller() == null || product.getSeller().getUser() == null ||
                !product.getSeller().getUser().getId().equals(userId)) {
            throw new UnauthorizedProductAccessException("You do not have permission to manage product ID: " + productId);
        }

        return product;
    }

    private SellerEntity resolveApprovedSeller(Long userId) {
        SellerEntity seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedProductAccessException("No seller profile associated with user ID: " + userId));

        if (seller.getApprovalStatus() != SellerApprovalStatus.APPROVED) {
            throw new UnauthorizedProductAccessException("Seller account is not approved. Current status: " + seller.getApprovalStatus());
        }
        return seller;
    }

    private SellerEntity resolveSeller(Long userId) {
        return sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedProductAccessException("No seller profile associated with user ID: " + userId));
    }
}
