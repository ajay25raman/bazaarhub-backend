package com.bazaarhub.backend.delivery.infrastructure.persistence.repository;

import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.ShipmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataShipmentRepository extends JpaRepository<ShipmentEntity, Long> {

    Optional<ShipmentEntity> findByShipmentNumber(String shipmentNumber);

    Optional<ShipmentEntity> findByTrackingNumber(String trackingNumber);

    boolean existsByShipmentNumber(String shipmentNumber);

    boolean existsBySellerOrderId(Long sellerOrderId);

    List<ShipmentEntity> findBySellerOrderId(Long sellerOrderId);

    List<ShipmentEntity> findBySellerOrderOrderId(Long orderId);

    Optional<ShipmentEntity> findFirstBySellerOrderIdOrderByCreatedAtDesc(Long sellerOrderId);

    Page<ShipmentEntity> findByStatus(ShipmentStatus status, Pageable pageable);

    Page<ShipmentEntity> findByStatusIn(List<ShipmentStatus> statuses, Pageable pageable);

    Page<ShipmentEntity> findBySellerOrderSellerId(Long sellerId, Pageable pageable);

    Page<ShipmentEntity> findBySellerOrderSellerIdAndStatus(Long sellerId, ShipmentStatus status, Pageable pageable);

    @Query("SELECT s FROM ShipmentEntity s WHERE " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:sellerOrderId IS NULL OR s.sellerOrder.id = :sellerOrderId)")
    Page<ShipmentEntity> searchShipments(
            @Param("status") ShipmentStatus status,
            @Param("sellerOrderId") Long sellerOrderId,
            Pageable pageable);

    @Query("SELECT s FROM ShipmentEntity s WHERE s.sellerOrder.order.user.id = :customerId ORDER BY s.createdAt DESC")
    Page<ShipmentEntity> findByCustomerId(@Param("customerId") Long customerId, Pageable pageable);

    @Query("SELECT s FROM ShipmentEntity s WHERE " +
           "(:search IS NULL OR LOWER(s.shipmentNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.trackingNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.sellerOrder.subOrderNumber) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:sellerId IS NULL OR s.sellerOrder.seller.id = :sellerId) AND " +
           "(:customerId IS NULL OR s.sellerOrder.order.user.id = :customerId)")
    Page<ShipmentEntity> adminSearchShipments(
            @Param("search") String search,
            @Param("status") ShipmentStatus status,
            @Param("sellerId") Long sellerId,
            @Param("customerId") Long customerId,
            Pageable pageable);

    @Query("SELECT COUNT(s) FROM ShipmentEntity s WHERE s.createdAt BETWEEN :start AND :end")
    long countShipmentsBetween(@Param("start") java.time.Instant start, @Param("end") java.time.Instant end);

    @Query("SELECT COUNT(s) FROM ShipmentEntity s WHERE s.status = :status AND s.createdAt BETWEEN :start AND :end")
    long countShipmentsByStatusBetween(@Param("status") ShipmentStatus status, @Param("start") java.time.Instant start, @Param("end") java.time.Instant end);

    @Query("SELECT s.status, COUNT(s) FROM ShipmentEntity s WHERE s.createdAt BETWEEN :start AND :end GROUP BY s.status")
    List<Object[]> aggregateShipmentStatusCounts(@Param("start") java.time.Instant start, @Param("end") java.time.Instant end);
}
