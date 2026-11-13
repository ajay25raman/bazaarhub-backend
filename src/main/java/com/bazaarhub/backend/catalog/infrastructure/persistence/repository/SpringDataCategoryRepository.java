package com.bazaarhub.backend.catalog.infrastructure.persistence.repository;

import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataCategoryRepository extends JpaRepository<CategoryEntity, Long>, JpaSpecificationExecutor<CategoryEntity> {

    Optional<CategoryEntity> findBySlug(String slug);

    Optional<CategoryEntity> findBySlugAndStatus(String slug, CatalogStatus status);

    Optional<CategoryEntity> findByIdAndStatus(Long id, CatalogStatus status);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<CategoryEntity> findByParentIsNullOrderByDisplayOrderAscNameAsc();

    List<CategoryEntity> findByParentIsNullAndStatusOrderByDisplayOrderAscNameAsc(CatalogStatus status);

    List<CategoryEntity> findByParentIdOrderByDisplayOrderAscNameAsc(Long parentId);

    List<CategoryEntity> findByParentIdAndStatusOrderByDisplayOrderAscNameAsc(Long parentId, CatalogStatus status);

    List<CategoryEntity> findByStatusOrderByDisplayOrderAscNameAsc(CatalogStatus status);

    List<CategoryEntity> findAllByOrderByDisplayOrderAscNameAsc();

    Page<CategoryEntity> findByStatus(CatalogStatus status, Pageable pageable);

    Page<CategoryEntity> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    Page<CategoryEntity> findByNameContainingIgnoreCaseAndStatus(String keyword, CatalogStatus status, Pageable pageable);

    Page<CategoryEntity> findByParentId(Long parentId, Pageable pageable);

    Page<CategoryEntity> findByParentIdAndStatus(Long parentId, CatalogStatus status, Pageable pageable);

    boolean existsByParentId(Long parentId);

    @Query(value = "SELECT COUNT(*) FROM products WHERE category_id = :categoryId", nativeQuery = true)
    long countProductsByCategoryId(@Param("categoryId") Long categoryId);

    @Query(value = "SELECT COUNT(*) FROM products WHERE category_id = :categoryId AND status = 'ACTIVE'", nativeQuery = true)
    long countActiveProductsByCategoryId(@Param("categoryId") Long categoryId);

    @Query(value = "SELECT COUNT(*) FROM products WHERE category_id IN (:categoryIds) AND status = 'ACTIVE'", nativeQuery = true)
    long countActiveProductsByCategoryIds(@Param("categoryIds") Collection<Long> categoryIds);
}
