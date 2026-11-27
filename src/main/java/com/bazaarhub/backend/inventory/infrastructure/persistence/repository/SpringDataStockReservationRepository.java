package com.bazaarhub.backend.inventory.infrastructure.persistence.repository;

import com.bazaarhub.backend.inventory.domain.model.ReservationStatus;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.StockReservationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataStockReservationRepository extends JpaRepository<StockReservationEntity, Long>, JpaSpecificationExecutor<StockReservationEntity> {

    Optional<StockReservationEntity> findByCheckoutReferenceAndVariantId(String checkoutReference, Long variantId);

    List<StockReservationEntity> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant cutoff);

    Page<StockReservationEntity> findByVariantId(Long variantId, Pageable pageable);

    Page<StockReservationEntity> findByStatus(ReservationStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE StockReservationEntity r SET r.status = :newStatus, r.updatedAt = :now WHERE r.id = :id AND r.status = :expectedStatus")
    int transitionStatusSafely(@Param("id") Long id,
                               @Param("expectedStatus") ReservationStatus expectedStatus,
                               @Param("newStatus") ReservationStatus newStatus,
                               @Param("now") Instant now);
}
