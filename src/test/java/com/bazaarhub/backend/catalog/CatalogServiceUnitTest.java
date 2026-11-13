package com.bazaarhub.backend.catalog;

import com.bazaarhub.backend.catalog.application.dto.request.*;
import com.bazaarhub.backend.catalog.application.dto.response.*;
import com.bazaarhub.backend.catalog.application.service.BrandService;
import com.bazaarhub.backend.catalog.application.service.CategoryService;
import com.bazaarhub.backend.catalog.domain.exception.*;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.BrandEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.CategoryEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataBrandRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataCategoryRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceUnitTest {

    @Mock
    private SpringDataCategoryRepository categoryRepository;
    @Mock
    private SpringDataBrandRepository brandRepository;
    @Mock
    private SpringDataUserRepository userRepository;

    private CategoryService categoryService;
    private BrandService brandService;

    private UserEntity adminUser;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository, userRepository);
        brandService = new BrandService(brandRepository, userRepository);

        adminUser = UserEntity.builder()
                .id(1L)
                .fullName("System Admin")
                .email("admin@bazaarhub.com")
                .build();
    }

    // =========================================================================
    // Category Tests
    // =========================================================================

    @Test
    @DisplayName("Create root category successfully")
    void createCategory_root_success() {
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .name("Electronics")
                .slug("electronics")
                .description("Consumer electronics")
                .displayOrder(1)
                .build();

        when(categoryRepository.existsBySlug("electronics")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(categoryRepository.save(any(CategoryEntity.class))).thenAnswer(inv -> {
            CategoryEntity e = inv.getArgument(0);
            e.setId(10L);
            e.setCreatedAt(Instant.now());
            e.setUpdatedAt(Instant.now());
            return e;
        });

        CategoryResponse response = categoryService.createCategory(request, 1L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Electronics", response.getName());
        assertEquals("electronics", response.getSlug());
        assertEquals(CatalogStatus.ACTIVE, response.getStatus());
        assertNull(response.getParentId());
        verify(categoryRepository).save(any(CategoryEntity.class));
    }

    @Test
    @DisplayName("Create category with duplicate slug throws CategoryAlreadyExistsException")
    void createCategory_duplicateSlug_throwsConflict() {
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .name("Electronics")
                .slug("electronics")
                .build();

        when(categoryRepository.existsBySlug("electronics")).thenReturn(true);

        assertThrows(CategoryAlreadyExistsException.class, () ->
                categoryService.createCategory(request, 1L));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create child category under inactive parent throws InvalidCategoryHierarchyException")
    void createCategory_inactiveParent_throwsInvalidHierarchy() {
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .name("Smartphones")
                .parentId(10L)
                .build();

        CategoryEntity inactiveParent = CategoryEntity.builder()
                .id(10L)
                .name("Old Electronics")
                .status(CatalogStatus.INACTIVE)
                .build();

        when(categoryRepository.existsBySlug(any())).thenReturn(false);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(inactiveParent));

        assertThrows(InvalidCategoryHierarchyException.class, () ->
                categoryService.createCategory(request, 1L));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update category to make it its own parent throws InvalidCategoryHierarchyException")
    void updateCategory_selfParent_throwsInvalidHierarchy() {
        CategoryEntity category = CategoryEntity.builder()
                .id(10L)
                .name("Electronics")
                .slug("electronics")
                .status(CatalogStatus.ACTIVE)
                .build();

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                .name("Electronics Updated")
                .parentId(10L)
                .build();

        assertThrows(InvalidCategoryHierarchyException.class, () ->
                categoryService.updateCategory(10L, request, 1L));
    }

    @Test
    @DisplayName("Update category introducing circular hierarchy throws CircularCategoryHierarchyException")
    void updateCategory_circularHierarchy_throwsCircularHierarchyException() {
        // Hierarchy: A (id:1) -> B (id:2) -> C (id:3)
        // Attempting to set A's parent as C (id:3) creates cycle: A -> C -> B -> A
        CategoryEntity catA = CategoryEntity.builder().id(1L).name("A").build();
        CategoryEntity catB = CategoryEntity.builder().id(2L).name("B").parent(catA).build();
        CategoryEntity catC = CategoryEntity.builder().id(3L).name("C").parent(catB).build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(catA));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(catC));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(catB));

        CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                .name("A")
                .parentId(3L)
                .build();

        assertThrows(CircularCategoryHierarchyException.class, () ->
                categoryService.updateCategory(1L, request, 1L));
    }

    @Test
    @DisplayName("Deactivating category with active products throws CategoryOperationNotAllowedException")
    void updateCategoryStatus_activeProducts_throwsNotAllowed() {
        CategoryEntity category = CategoryEntity.builder()
                .id(10L)
                .name("Electronics")
                .status(CatalogStatus.ACTIVE)
                .build();

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(categoryRepository.countActiveProductsByCategoryId(10L)).thenReturn(5L);

        assertThrows(CategoryOperationNotAllowedException.class, () ->
                categoryService.updateCategoryStatus(10L, CatalogStatus.INACTIVE, 1L));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deactivating category with active child categories throws CategoryOperationNotAllowedException")
    void updateCategoryStatus_activeChildren_throwsNotAllowed() {
        CategoryEntity category = CategoryEntity.builder()
                .id(10L)
                .name("Electronics")
                .status(CatalogStatus.ACTIVE)
                .build();

        CategoryEntity child = CategoryEntity.builder()
                .id(20L)
                .name("Laptops")
                .parent(category)
                .status(CatalogStatus.ACTIVE)
                .build();

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(categoryRepository.countActiveProductsByCategoryId(10L)).thenReturn(0L);
        when(categoryRepository.findByParentIdAndStatusOrderByDisplayOrderAscNameAsc(10L, CatalogStatus.ACTIVE))
                .thenReturn(List.of(child));

        assertThrows(CategoryOperationNotAllowedException.class, () ->
                categoryService.updateCategoryStatus(10L, CatalogStatus.INACTIVE, 1L));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get category hierarchy tree builds multi-level tree correctly")
    void getCategoryTree_buildsProperHierarchy() {
        CategoryEntity root1 = CategoryEntity.builder().id(1L).name("Electronics").displayOrder(1).status(CatalogStatus.ACTIVE).children(new ArrayList<>()).build();
        CategoryEntity root2 = CategoryEntity.builder().id(2L).name("Fashion").displayOrder(2).status(CatalogStatus.ACTIVE).children(new ArrayList<>()).build();
        CategoryEntity child1 = CategoryEntity.builder().id(3L).name("Smartphones").parent(root1).displayOrder(1).status(CatalogStatus.ACTIVE).children(new ArrayList<>()).build();
        CategoryEntity subChild1 = CategoryEntity.builder().id(4L).name("Android Phones").parent(child1).displayOrder(1).status(CatalogStatus.ACTIVE).children(new ArrayList<>()).build();

        when(categoryRepository.findByStatusOrderByDisplayOrderAscNameAsc(CatalogStatus.ACTIVE))
                .thenReturn(List.of(root1, root2, child1, subChild1));

        List<CategoryTreeResponse> tree = categoryService.getCategoryTree(true);

        assertEquals(2, tree.size());
        assertEquals("Electronics", tree.get(0).getName());
        assertEquals("Fashion", tree.get(1).getName());

        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals("Smartphones", tree.get(0).getChildren().get(0).getName());

        assertEquals(1, tree.get(0).getChildren().get(0).getChildren().size());
        assertEquals("Android Phones", tree.get(0).getChildren().get(0).getChildren().get(0).getName());
    }

    // =========================================================================
    // Brand Tests
    // =========================================================================

    @Test
    @DisplayName("Create brand successfully")
    void createBrand_success() {
        BrandCreateRequest request = BrandCreateRequest.builder()
                .name("Sony")
                .slug("sony")
                .logoUrl("https://cdn.bazaarhub.com/sony.png")
                .build();

        when(brandRepository.existsByName("Sony")).thenReturn(false);
        when(brandRepository.existsBySlug("sony")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(brandRepository.save(any(BrandEntity.class))).thenAnswer(inv -> {
            BrandEntity e = inv.getArgument(0);
            e.setId(5L);
            e.setCreatedAt(Instant.now());
            e.setUpdatedAt(Instant.now());
            return e;
        });

        BrandResponse response = brandService.createBrand(request, 1L);

        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertEquals("Sony", response.getName());
        assertEquals("sony", response.getSlug());
        assertEquals(CatalogStatus.ACTIVE, response.getStatus());
        verify(brandRepository).save(any(BrandEntity.class));
    }

    @Test
    @DisplayName("Create brand with duplicate name throws BrandAlreadyExistsException")
    void createBrand_duplicateName_throwsConflict() {
        BrandCreateRequest request = BrandCreateRequest.builder()
                .name("Sony")
                .build();

        when(brandRepository.existsByName("Sony")).thenReturn(true);

        assertThrows(BrandAlreadyExistsException.class, () ->
                brandService.createBrand(request, 1L));
        verify(brandRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create brand with duplicate slug throws BrandAlreadyExistsException")
    void createBrand_duplicateSlug_throwsConflict() {
        BrandCreateRequest request = BrandCreateRequest.builder()
                .name("Sony Electronics")
                .slug("sony")
                .build();

        when(brandRepository.existsByName("Sony Electronics")).thenReturn(false);
        when(brandRepository.existsBySlug("sony")).thenReturn(true);

        assertThrows(BrandAlreadyExistsException.class, () ->
                brandService.createBrand(request, 1L));
        verify(brandRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update brand with duplicate name in use by another brand throws BrandAlreadyExistsException")
    void updateBrand_duplicateName_throwsConflict() {
        BrandEntity brand = BrandEntity.builder()
                .id(5L)
                .name("Sony")
                .slug("sony")
                .build();

        when(brandRepository.findById(5L)).thenReturn(Optional.of(brand));
        when(brandRepository.existsByNameAndIdNot("Samsung", 5L)).thenReturn(true);

        BrandUpdateRequest request = BrandUpdateRequest.builder()
                .name("Samsung")
                .build();

        assertThrows(BrandAlreadyExistsException.class, () ->
                brandService.updateBrand(5L, request, 1L));
        verify(brandRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deactivate brand with active products throws BrandOperationNotAllowedException")
    void updateBrandStatus_activeProducts_throwsNotAllowed() {
        BrandEntity brand = BrandEntity.builder()
                .id(5L)
                .name("Sony")
                .status(CatalogStatus.ACTIVE)
                .build();

        when(brandRepository.findById(5L)).thenReturn(Optional.of(brand));
        when(brandRepository.countActiveProductsByBrandId(5L)).thenReturn(12L);

        assertThrows(BrandOperationNotAllowedException.class, () ->
                brandService.updateBrandStatus(5L, CatalogStatus.INACTIVE, 1L));
        verify(brandRepository, never()).save(any());
    }
}
