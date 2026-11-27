package com.bazaarhub.backend.delivery.infrastructure.persistence.repository;

import com.bazaarhub.backend.delivery.domain.model.DeliveryBoyAvailabilityStatus;
import com.bazaarhub.backend.delivery.domain.model.DeliveryBoyVerificationStatus;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryBoyEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataDeliveryBoyRepository extends JpaRepository<DeliveryBoyEntity, Long> {

    Optional<DeliveryBoyEntity> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    Page<DeliveryBoyEntity> findByVerificationStatus(DeliveryBoyVerificationStatus status, Pageable pageable);

    Page<DeliveryBoyEntity> findByAvailabilityStatus(DeliveryBoyAvailabilityStatus status, Pageable pageable);

    Page<DeliveryBoyEntity> findByVerificationStatusAndAvailabilityStatus(
            DeliveryBoyVerificationStatus verificationStatus,
            DeliveryBoyAvailabilityStatus availabilityStatus,
            Pageable pageable);

    @Query("SELECT d FROM DeliveryBoyEntity d WHERE " +
           "(:verificationStatus IS NULL OR d.verificationStatus = :verificationStatus) AND " +
           "(:availabilityStatus IS NULL OR d.availabilityStatus = :availabilityStatus)")
    Page<DeliveryBoyEntity> searchDeliveryBoys(
            @Param("verificationStatus") DeliveryBoyVerificationStatus verificationStatus,
            @Param("availabilityStatus") DeliveryBoyAvailabilityStatus availabilityStatus,
            Pageable pageable);

    List<DeliveryBoyEntity> findByVerificationStatusAndAvailabilityStatus(
            DeliveryBoyVerificationStatus verificationStatus,
            DeliveryBoyAvailabilityStatus availabilityStatus);
}
