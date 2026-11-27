package com.bazaarhub.backend.inventory.infrastructure.persistence.repository;

import com.bazaarhub.backend.inventory.domain.model.MovementType;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryMovementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataInventoryMovementRepository extends JpaRepository<InventoryMovementEntity, Long>, JpaSpecificationExecutor<InventoryMovementEntity> {

    Page<InventoryMovementEntity> findByInventoryId(Long inventoryId, Pageable pageable);

    Page<InventoryMovementEntity> findByInventoryVariantId(Long variantId, Pageable pageable);

    Page<InventoryMovementEntity> findByMovementType(MovementType movementType, Pageable pageable);
}
