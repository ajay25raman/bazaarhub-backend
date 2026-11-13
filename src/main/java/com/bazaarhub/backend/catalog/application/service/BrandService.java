package com.bazaarhub.backend.catalog.application.service;

import com.bazaarhub.backend.catalog.application.dto.request.BrandCreateRequest;
import com.bazaarhub.backend.catalog.application.dto.request.BrandUpdateRequest;
import com.bazaarhub.backend.catalog.application.dto.response.BrandResponse;
import com.bazaarhub.backend.catalog.domain.exception.BrandAlreadyExistsException;
import com.bazaarhub.backend.catalog.domain.exception.BrandNotFoundException;
import com.bazaarhub.backend.catalog.domain.exception.BrandOperationNotAllowedException;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.BrandEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataBrandRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
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
public class BrandService {

    private static final Logger log = LoggerFactory.getLogger(BrandService.class);

    private final SpringDataBrandRepository brandRepository;
    private final SpringDataUserRepository userRepository;

    public BrandService(SpringDataBrandRepository brandRepository,
                        SpringDataUserRepository userRepository) {
        this.brandRepository = brandRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BrandResponse createBrand(BrandCreateRequest request, Long adminUserId) {
        String cleanName = request.getName().trim();

        // 1. Uniqueness check for brand name
        if (brandRepository.existsByName(cleanName)) {
            throw new BrandAlreadyExistsException("Brand with name '" + cleanName + "' already exists");
        }

        // 2. Resolve and validate slug
        String rawSlug = request.getSlug() != null && !request.getSlug().isBlank()
                ? SlugUtils.toSlug(request.getSlug())
                : SlugUtils.toSlug(cleanName);

        if (rawSlug.isBlank()) {
            rawSlug = "brand-" + System.currentTimeMillis();
        }

        if (brandRepository.existsBySlug(rawSlug)) {
            throw new BrandAlreadyExistsException("Brand slug '" + rawSlug + "' is already in use");
        }

        // 3. Resolve audit user
        UserEntity adminUser = resolveUser(adminUserId);

        // 4. Build and persist entity
        BrandEntity brand = BrandEntity.builder()
                .name(cleanName)
                .slug(rawSlug)
                .logoUrl(request.getLogoUrl() != null ? request.getLogoUrl().trim() : null)
                .status(request.getStatus() != null ? request.getStatus() : CatalogStatus.ACTIVE)
                .createdBy(adminUser)
                .updatedBy(adminUser)
                .build();

        BrandEntity saved = brandRepository.save(brand);
        log.info("[BRAND CREATED] Created brand ID: {} ('{}') with slug '{}'",
                saved.getId(), saved.getName(), saved.getSlug());

        return mapToResponse(saved);
    }

    @Transactional
    public BrandResponse updateBrand(Long id, BrandUpdateRequest request, Long adminUserId) {
        BrandEntity brand = brandRepository.findById(id)
                .orElseThrow(() -> new BrandNotFoundException(id));

        String cleanName = request.getName().trim();

        // 1. Uniqueness check for brand name
        if (!brand.getName().equalsIgnoreCase(cleanName) && brandRepository.existsByNameAndIdNot(cleanName, id)) {
            throw new BrandAlreadyExistsException("Brand name '" + cleanName + "' is already in use by another brand");
        }

        // 2. Resolve slug changes
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            String newSlug = SlugUtils.toSlug(request.getSlug());
            if (brandRepository.existsBySlugAndIdNot(newSlug, id)) {
                throw new BrandAlreadyExistsException("Brand slug '" + newSlug + "' is already in use by another brand");
            }
            brand.setSlug(newSlug);
        }

        // 3. Update descriptive fields
        brand.setName(cleanName);
        if (request.getLogoUrl() != null) {
            brand.setLogoUrl(request.getLogoUrl().trim());
        }

        // 4. Update audit user
        UserEntity adminUser = resolveUser(adminUserId);
        if (adminUser != null) {
            brand.setUpdatedBy(adminUser);
        }

        BrandEntity updated = brandRepository.save(brand);
        log.info("[BRAND UPDATED] Updated brand ID: {} ('{}')", updated.getId(), updated.getName());

        return mapToResponse(updated);
    }

    @Transactional
    public BrandResponse updateBrandStatus(Long id, CatalogStatus newStatus, Long adminUserId) {
        BrandEntity brand = brandRepository.findById(id)
                .orElseThrow(() -> new BrandNotFoundException(id));

        if (brand.getStatus() == newStatus) {
            return mapToResponse(brand);
        }

        // Deactivation guard: check active products
        if (newStatus == CatalogStatus.INACTIVE) {
            long activeProducts = safeCountActiveProducts(id);
            if (activeProducts > 0) {
                throw new BrandOperationNotAllowedException(
                        "Cannot deactivate brand '" + brand.getName() + "' (ID: " + id +
                                ") because it has " + activeProducts + " active product(s) linked to it");
            }
        }

        brand.setStatus(newStatus);
        UserEntity adminUser = resolveUser(adminUserId);
        if (adminUser != null) {
            brand.setUpdatedBy(adminUser);
        }

        BrandEntity saved = brandRepository.save(brand);
        log.info("[BRAND STATUS UPDATED] Brand ID: {} status changed to: {}", saved.getId(), saved.getStatus());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public BrandResponse getBrandById(Long id) {
        BrandEntity brand = brandRepository.findById(id)
                .orElseThrow(() -> new BrandNotFoundException(id));
        return mapToResponse(brand);
    }

    @Transactional(readOnly = true)
    public BrandResponse getPublicBrandById(Long id) {
        BrandEntity brand = brandRepository.findByIdAndStatus(id, CatalogStatus.ACTIVE)
                .orElseThrow(() -> new BrandNotFoundException("Active brand with ID " + id + " was not found"));
        return mapToResponse(brand);
    }

    @Transactional(readOnly = true)
    public BrandResponse getBrandBySlug(String slug) {
        BrandEntity brand = brandRepository.findBySlug(slug)
                .orElseThrow(() -> new BrandNotFoundException("slug", slug));
        return mapToResponse(brand);
    }

    @Transactional(readOnly = true)
    public BrandResponse getPublicBrandBySlug(String slug) {
        BrandEntity brand = brandRepository.findBySlugAndStatus(slug, CatalogStatus.ACTIVE)
                .orElseThrow(() -> new BrandNotFoundException("slug", slug));
        return mapToResponse(brand);
    }

    @Transactional(readOnly = true)
    public Page<BrandResponse> getBrands(String keyword, CatalogStatus status, Pageable pageable) {
        Specification<BrandEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String searchPattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), searchPattern),
                        cb.like(cb.lower(root.get("slug")), searchPattern)
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return brandRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<BrandResponse> getActiveBrands() {
        return brandRepository.findByStatusOrderByNameAsc(CatalogStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private long safeCountActiveProducts(Long brandId) {
        try {
            return brandRepository.countActiveProductsByBrandId(brandId);
        } catch (Exception ex) {
            log.debug("[PRODUCT CHECK] Products table check skipped/unavailable: {}", ex.getMessage());
            return 0L;
        }
    }

    private UserEntity resolveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }

    public BrandResponse mapToResponse(BrandEntity brand) {
        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .slug(brand.getSlug())
                .logoUrl(brand.getLogoUrl())
                .status(brand.getStatus())
                .createdAt(brand.getCreatedAt())
                .updatedAt(brand.getUpdatedAt())
                .build();
    }
}
