package com.bazaarhub.backend.delivery.infrastructure.persistence.entity;

import com.bazaarhub.backend.delivery.domain.model.DeliveryBoyAvailabilityStatus;
import com.bazaarhub.backend.delivery.domain.model.DeliveryBoyVerificationStatus;
import com.bazaarhub.backend.delivery.domain.model.VehicleType;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity mapping strictly to the existing MariaDB `delivery_boys` table.
 */
@Entity
@Table(name = "delivery_boys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryBoyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, columnDefinition = "enum('BIKE','SCOOTER','BICYCLE','OTHER')")
    private VehicleType vehicleType;

    @Column(name = "vehicle_number", length = 30)
    private String vehicleNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, columnDefinition = "enum('PENDING','VERIFIED','REJECTED')")
    @Builder.Default
    private DeliveryBoyVerificationStatus verificationStatus = DeliveryBoyVerificationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, columnDefinition = "enum('AVAILABLE','BUSY','OFFLINE','SUSPENDED')")
    @Builder.Default
    private DeliveryBoyAvailabilityStatus availabilityStatus = DeliveryBoyAvailabilityStatus.OFFLINE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private UserEntity verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private UserEntity updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.verificationStatus == null) {
            this.verificationStatus = DeliveryBoyVerificationStatus.PENDING;
        }
        if (this.availabilityStatus == null) {
            this.availabilityStatus = DeliveryBoyAvailabilityStatus.OFFLINE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeliveryBoyEntity that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
