package com.bazaarhub.backend.catalog.infrastructure.persistence.repository;

import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.BrandEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataBrandRepository extends JpaRepository<BrandEntity, Long>, JpaSpecificationExecutor<BrandEntity> {

    Optional<BrandEntity> findBySlug(String slug);

    Optional<BrandEntity> findByIdAndStatus(Long id, CatalogStatus status);

    Optional<BrandEntity> findBySlugAndStatus(String slug, CatalogStatus status);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    List<BrandEntity> findByStatusOrderByNameAsc(CatalogStatus status);

    Page<BrandEntity> findByStatus(CatalogStatus status, Pageable pageable);

    Page<BrandEntity> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    Page<BrandEntity> findByNameContainingIgnoreCaseAndStatus(String keyword, CatalogStatus status, Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM products WHERE brand_id = :brandId", nativeQuery = true)
    long countProductsByBrandId(@Param("brandId") Long brandId);

    @Query(value = "SELECT COUNT(*) FROM products WHERE brand_id = :brandId AND status = 'ACTIVE'", nativeQuery = true)
    long countActiveProductsByBrandId(@Param("brandId") Long brandId);
}
