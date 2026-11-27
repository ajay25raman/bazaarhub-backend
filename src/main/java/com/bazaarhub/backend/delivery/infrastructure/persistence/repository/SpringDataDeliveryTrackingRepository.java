package com.bazaarhub.backend.delivery.infrastructure.persistence.repository;

import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryTrackingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataDeliveryTrackingRepository extends JpaRepository<DeliveryTrackingEntity, Long> {

    List<DeliveryTrackingEntity> findByShipmentIdOrderByRecordedAtDesc(Long shipmentId);

    Page<DeliveryTrackingEntity> findByShipmentIdOrderByRecordedAtDesc(Long shipmentId, Pageable pageable);

    Optional<DeliveryTrackingEntity> findFirstByShipmentIdOrderByRecordedAtDesc(Long shipmentId);

    List<DeliveryTrackingEntity> findByDeliveryBoyIdOrderByRecordedAtDesc(Long deliveryBoyId);
}
