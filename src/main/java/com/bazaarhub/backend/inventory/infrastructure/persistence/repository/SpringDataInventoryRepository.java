package com.bazaarhub.backend.inventory.infrastructure.persistence.repository;

import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataInventoryRepository extends JpaRepository<InventoryEntity, Long>, JpaSpecificationExecutor<InventoryEntity> {

    Optional<InventoryEntity> findByVariantId(Long variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryEntity i WHERE i.variant.id = :variantId")
    Optional<InventoryEntity> findByVariantIdWithLock(@Param("variantId") Long variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryEntity i WHERE i.id = :id")
    Optional<InventoryEntity> findByIdWithLock(@Param("id") Long id);

    boolean existsByVariantId(Long variantId);

    @Query("SELECT i FROM InventoryEntity i JOIN i.variant v JOIN v.product p WHERE p.seller.id = :sellerId")
    Page<InventoryEntity> findBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);

    @Query("SELECT COUNT(i) FROM InventoryEntity i WHERE (i.totalQuantity - i.reservedQuantity) <= 0")
    long countOutOfStock();

    @Query("SELECT COUNT(i) FROM InventoryEntity i WHERE (i.totalQuantity - i.reservedQuantity) > 0 AND (i.totalQuantity - i.reservedQuantity) <= i.safetyStock")
    long countLowStock();

    @Query("SELECT COUNT(i) FROM InventoryEntity i JOIN i.variant v JOIN v.product p WHERE p.seller.id = :sellerId AND (i.totalQuantity - i.reservedQuantity) <= 0")
    long countSellerOutOfStock(@Param("sellerId") Long sellerId);

    @Query("SELECT COUNT(i) FROM InventoryEntity i JOIN i.variant v JOIN v.product p WHERE p.seller.id = :sellerId AND (i.totalQuantity - i.reservedQuantity) > 0 AND (i.totalQuantity - i.reservedQuantity) <= i.safetyStock")
    long countSellerLowStock(@Param("sellerId") Long sellerId);
}
