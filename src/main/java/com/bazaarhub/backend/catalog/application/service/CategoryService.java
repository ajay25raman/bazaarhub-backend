package com.bazaarhub.backend.catalog.application.service;

import com.bazaarhub.backend.catalog.application.dto.request.CategoryCreateRequest;
import com.bazaarhub.backend.catalog.application.dto.request.CategoryUpdateRequest;
import com.bazaarhub.backend.catalog.application.dto.response.CategoryResponse;
import com.bazaarhub.backend.catalog.application.dto.response.CategoryTreeResponse;
import com.bazaarhub.backend.catalog.domain.exception.CategoryAlreadyExistsException;
import com.bazaarhub.backend.catalog.domain.exception.CategoryNotFoundException;
import com.bazaarhub.backend.catalog.domain.exception.CategoryOperationNotAllowedException;
import com.bazaarhub.backend.catalog.domain.exception.CircularCategoryHierarchyException;
import com.bazaarhub.backend.catalog.domain.exception.InvalidCategoryHierarchyException;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.CategoryEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataCategoryRepository;
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

import java.util.*;

@Service
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final SpringDataCategoryRepository categoryRepository;
    private final SpringDataUserRepository userRepository;

    public CategoryService(SpringDataCategoryRepository categoryRepository,
                           SpringDataUserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request, Long adminUserId) {
        String cleanName = request.getName().trim();

        // 1. Resolve and validate slug
        String rawSlug = request.getSlug() != null && !request.getSlug().isBlank()
                ? SlugUtils.toSlug(request.getSlug())
                : SlugUtils.toSlug(cleanName);

        if (rawSlug.isBlank()) {
            rawSlug = "category-" + System.currentTimeMillis();
        }

        if (categoryRepository.existsBySlug(rawSlug)) {
            throw new CategoryAlreadyExistsException("Category slug '" + rawSlug + "' is already in use");
        }

        // 2. Validate parent category if specified
        CategoryEntity parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new CategoryNotFoundException("Parent category with ID " + request.getParentId() + " not found"));

            if (parent.getStatus() == CatalogStatus.INACTIVE) {
                throw new InvalidCategoryHierarchyException("Cannot create a category under an INACTIVE parent category");
            }
        }

        // 3. Resolve audit user
        UserEntity adminUser = resolveUser(adminUserId);

        // 4. Build and persist entity
        CategoryEntity category = CategoryEntity.builder()
                .name(cleanName)
                .slug(rawSlug)
                .parent(parent)
                .description(request.getDescription())
                .imageUrl(request.getImageUrl() != null ? request.getImageUrl().trim() : null)
                .status(request.getStatus() != null ? request.getStatus() : CatalogStatus.ACTIVE)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .createdBy(adminUser)
                .updatedBy(adminUser)
                .build();

        CategoryEntity saved = categoryRepository.save(category);
        log.info("[CATEGORY CREATED] Created category ID: {} ('{}') with slug '{}'",
                saved.getId(), saved.getName(), saved.getSlug());

        return mapToResponse(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryUpdateRequest request, Long adminUserId) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        String cleanName = request.getName().trim();

        // 1. Resolve slug changes
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            String newSlug = SlugUtils.toSlug(request.getSlug());
            if (categoryRepository.existsBySlugAndIdNot(newSlug, id)) {
                throw new CategoryAlreadyExistsException("Category slug '" + newSlug + "' is already in use by another category");
            }
            category.setSlug(newSlug);
        }

        // 2. Validate parent hierarchy and prevent circular references
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new InvalidCategoryHierarchyException("A category cannot be its own parent");
            }

            CategoryEntity newParent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new CategoryNotFoundException("Parent category with ID " + request.getParentId() + " not found"));

            validateNoCircularHierarchy(id, request.getParentId());
            category.setParent(newParent);
        } else {
            category.setParent(null); // Converted to root category
        }

        // 3. Update descriptive fields
        category.setName(cleanName);
        category.setDescription(request.getDescription());
        if (request.getImageUrl() != null) {
            category.setImageUrl(request.getImageUrl().trim());
        }
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }

        // 4. Update audit user
        UserEntity adminUser = resolveUser(adminUserId);
        if (adminUser != null) {
            category.setUpdatedBy(adminUser);
        }

        CategoryEntity updated = categoryRepository.save(category);
        log.info("[CATEGORY UPDATED] Updated category ID: {} ('{}')", updated.getId(), updated.getName());

        return mapToResponse(updated);
    }

    @Transactional
    public CategoryResponse updateCategoryStatus(Long id, CatalogStatus newStatus, Long adminUserId) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        if (category.getStatus() == newStatus) {
            return mapToResponse(category);
        }

        // Deactivation guard: check active products and active children
        if (newStatus == CatalogStatus.INACTIVE) {
            long activeProducts = safeCountActiveProducts(id);
            if (activeProducts > 0) {
                throw new CategoryOperationNotAllowedException(
                        "Cannot deactivate category '" + category.getName() + "' (ID: " + id +
                                ") because it has " + activeProducts + " active product(s) linked to it");
            }

            List<CategoryEntity> activeChildren = categoryRepository.findByParentIdAndStatusOrderByDisplayOrderAscNameAsc(id, CatalogStatus.ACTIVE);
            if (!activeChildren.isEmpty()) {
                throw new CategoryOperationNotAllowedException(
                        "Cannot deactivate category '" + category.getName() + "' because it has " +
                                activeChildren.size() + " active subcategory/subcategories. Please deactivate or reassign child subcategories first.");
            }
        }

        // Activation guard: parent must not be inactive
        if (newStatus == CatalogStatus.ACTIVE && category.getParent() != null) {
            if (category.getParent().getStatus() == CatalogStatus.INACTIVE) {
                throw new CategoryOperationNotAllowedException(
                        "Cannot activate category '" + category.getName() + "' while its parent category '" +
                                category.getParent().getName() + "' is INACTIVE. Please activate parent category first.");
            }
        }

        category.setStatus(newStatus);
        UserEntity adminUser = resolveUser(adminUserId);
        if (adminUser != null) {
            category.setUpdatedBy(adminUser);
        }

        CategoryEntity saved = categoryRepository.save(category);
        log.info("[CATEGORY STATUS UPDATED] Category ID: {} status changed to: {}", saved.getId(), saved.getStatus());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        return mapToResponse(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getPublicCategoryById(Long id) {
        CategoryEntity category = categoryRepository.findByIdAndStatus(id, CatalogStatus.ACTIVE)
                .orElseThrow(() -> new CategoryNotFoundException("Active category with ID " + id + " was not found"));
        return mapToResponse(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        CategoryEntity category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new CategoryNotFoundException("slug", slug));
        return mapToResponse(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getPublicCategoryBySlug(String slug) {
        CategoryEntity category = categoryRepository.findBySlugAndStatus(slug, CatalogStatus.ACTIVE)
                .orElseThrow(() -> new CategoryNotFoundException("slug", slug));
        return mapToResponse(category);
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> getCategories(String keyword, Long parentId, CatalogStatus status, Pageable pageable) {
        Specification<CategoryEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String searchPattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), searchPattern),
                        cb.like(cb.lower(root.get("slug")), searchPattern),
                        cb.like(cb.lower(root.get("description")), searchPattern)
                ));
            }

            if (parentId != null) {
                if (parentId == 0L) {
                    predicates.add(cb.isNull(root.get("parent")));
                } else {
                    predicates.add(cb.equal(root.get("parent").get("id"), parentId));
                }
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return categoryRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getRootCategories(boolean activeOnly) {
        List<CategoryEntity> roots = activeOnly
                ? categoryRepository.findByParentIsNullAndStatusOrderByDisplayOrderAscNameAsc(CatalogStatus.ACTIVE)
                : categoryRepository.findByParentIsNullOrderByDisplayOrderAscNameAsc();

        return roots.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getChildCategories(Long parentId, boolean activeOnly) {
        if (!categoryRepository.existsById(parentId)) {
            throw new CategoryNotFoundException(parentId);
        }

        List<CategoryEntity> children = activeOnly
                ? categoryRepository.findByParentIdAndStatusOrderByDisplayOrderAscNameAsc(parentId, CatalogStatus.ACTIVE)
                : categoryRepository.findByParentIdOrderByDisplayOrderAscNameAsc(parentId);

        return children.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> getCategoryTree(boolean activeOnly) {
        List<CategoryEntity> allCategories = activeOnly
                ? categoryRepository.findByStatusOrderByDisplayOrderAscNameAsc(CatalogStatus.ACTIVE)
                : categoryRepository.findAllByOrderByDisplayOrderAscNameAsc();

        Map<Long, CategoryTreeResponse> nodeMap = new LinkedHashMap<>();
        for (CategoryEntity entity : allCategories) {
            nodeMap.put(entity.getId(), mapToTreeNode(entity));
        }

        List<CategoryTreeResponse> rootNodes = new ArrayList<>();
        for (CategoryEntity entity : allCategories) {
            CategoryTreeResponse node = nodeMap.get(entity.getId());
            if (entity.getParent() != null && nodeMap.containsKey(entity.getParent().getId())) {
                nodeMap.get(entity.getParent().getId()).getChildren().add(node);
            } else {
                rootNodes.add(node);
            }
        }

        return rootNodes;
    }

    private void validateNoCircularHierarchy(Long categoryId, Long proposedParentId) {
        Long currentAncestorId = proposedParentId;
        Set<Long> visited = new HashSet<>();

        while (currentAncestorId != null) {
            if (currentAncestorId.equals(categoryId)) {
                throw new CircularCategoryHierarchyException(
                        "Circular category relationship detected: category ID " + categoryId +
                                " is an ancestor of proposed parent ID " + proposedParentId);
            }

            if (!visited.add(currentAncestorId)) {
                break;
            }

            CategoryEntity ancestor = categoryRepository.findById(currentAncestorId).orElse(null);
            currentAncestorId = (ancestor != null && ancestor.getParent() != null)
                    ? ancestor.getParent().getId()
                    : null;
        }
    }

    private long safeCountActiveProducts(Long categoryId) {
        try {
            return categoryRepository.countActiveProductsByCategoryId(categoryId);
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

    public CategoryResponse mapToResponse(CategoryEntity category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .status(category.getStatus())
                .displayOrder(category.getDisplayOrder())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private CategoryTreeResponse mapToTreeNode(CategoryEntity category) {
        return CategoryTreeResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .status(category.getStatus())
                .displayOrder(category.getDisplayOrder())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .children(new ArrayList<>())
                .build();
    }
}
