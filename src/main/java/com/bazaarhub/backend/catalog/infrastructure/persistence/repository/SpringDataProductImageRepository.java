package com.bazaarhub.backend.catalog.infrastructure.persistence.repository;

import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataProductImageRepository extends JpaRepository<ProductImageEntity, Long> {

    List<ProductImageEntity> findByProductIdOrderByDisplayOrderAscIdAsc(Long productId);

    Optional<ProductImageEntity> findByIdAndProductId(Long id, Long productId);

    Optional<ProductImageEntity> findByProductIdAndIsPrimaryTrue(Long productId);

    Optional<ProductImageEntity> findFirstByProductIdOrderByDisplayOrderAscIdAsc(Long productId);

    boolean existsByProductId(Long productId);

    long countByProductId(Long productId);

    @Modifying
    @Query("UPDATE ProductImageEntity img SET img.isPrimary = false WHERE img.product.id = :productId")
    void resetPrimaryFlags(@Param("productId") Long productId);
}
