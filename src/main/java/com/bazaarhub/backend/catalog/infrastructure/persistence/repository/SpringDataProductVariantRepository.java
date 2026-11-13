package com.bazaarhub.backend.catalog.infrastructure.persistence.repository;

import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataProductVariantRepository extends JpaRepository<ProductVariantEntity, Long> {

    List<ProductVariantEntity> findByProductId(Long productId);

    Optional<ProductVariantEntity> findByIdAndProductId(Long id, Long productId);

    Optional<ProductVariantEntity> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    long countByProductId(Long productId);
}
