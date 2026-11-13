package com.bazaarhub.backend.catalog.infrastructure.persistence.repository;

import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataProductRepository extends JpaRepository<ProductEntity, Long>, JpaSpecificationExecutor<ProductEntity> {

    Optional<ProductEntity> findBySlug(String slug);

    Optional<ProductEntity> findByIdAndStatusAndApprovalStatus(Long id, ProductStatus status, ProductApprovalStatus approvalStatus);

    Optional<ProductEntity> findBySlugAndStatusAndApprovalStatus(String slug, ProductStatus status, ProductApprovalStatus approvalStatus);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    Page<ProductEntity> findBySellerId(Long sellerId, Pageable pageable);

    Page<ProductEntity> findByCategoryIdAndStatusAndApprovalStatus(Long categoryId, ProductStatus status, ProductApprovalStatus approvalStatus, Pageable pageable);

    Page<ProductEntity> findByBrandIdAndStatusAndApprovalStatus(Long brandId, ProductStatus status, ProductApprovalStatus approvalStatus, Pageable pageable);
}
