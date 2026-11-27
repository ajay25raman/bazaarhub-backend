package com.bazaarhub.backend.delivery.infrastructure.persistence.repository;

import com.bazaarhub.backend.delivery.domain.model.DeliveryAssignmentStatus;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryAssignmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataDeliveryAssignmentRepository extends JpaRepository<DeliveryAssignmentEntity, Long> {

    List<DeliveryAssignmentEntity> findByShipmentId(Long shipmentId);

    Optional<DeliveryAssignmentEntity> findFirstByShipmentIdOrderByAssignedAtDesc(Long shipmentId);

    Optional<DeliveryAssignmentEntity> findFirstByShipmentIdAndAssignmentStatusNotOrderByAssignedAtDesc(
            Long shipmentId, DeliveryAssignmentStatus status);

    Optional<DeliveryAssignmentEntity> findFirstByShipmentIdAndAssignmentStatusIn(
            Long shipmentId, List<DeliveryAssignmentStatus> statuses);

    Page<DeliveryAssignmentEntity> findByDeliveryBoyId(Long deliveryBoyId, Pageable pageable);

    Page<DeliveryAssignmentEntity> findByDeliveryBoyIdAndAssignmentStatus(
            Long deliveryBoyId, DeliveryAssignmentStatus status, Pageable pageable);

    Page<DeliveryAssignmentEntity> findByDeliveryBoyIdAndAssignmentStatusIn(
            Long deliveryBoyId, List<DeliveryAssignmentStatus> statuses, Pageable pageable);

    @Query("SELECT a FROM DeliveryAssignmentEntity a WHERE " +
           "(:deliveryBoyId IS NULL OR a.deliveryBoy.id = :deliveryBoyId) AND " +
           "(:shipmentId IS NULL OR a.shipment.id = :shipmentId) AND " +
           "(:status IS NULL OR a.assignmentStatus = :status)")
    Page<DeliveryAssignmentEntity> searchAssignments(
            @Param("deliveryBoyId") Long deliveryBoyId,
            @Param("shipmentId") Long shipmentId,
            @Param("status") DeliveryAssignmentStatus status,
            Pageable pageable);

    long countByDeliveryBoyIdAndAssignmentStatusIn(
            Long deliveryBoyId, List<DeliveryAssignmentStatus> statuses);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
           "FROM DeliveryAssignmentEntity a " +
           "WHERE a.shipment.id = :shipmentId " +
           "AND a.deliveryBoy.user.id = :userId " +
           "AND a.assignmentStatus NOT IN :excludedStatuses")
    boolean isDeliveryBoyAssignedToShipment(
            @Param("shipmentId") Long shipmentId,
            @Param("userId") Long userId,
            @Param("excludedStatuses") Collection<DeliveryAssignmentStatus> excludedStatuses);
}
