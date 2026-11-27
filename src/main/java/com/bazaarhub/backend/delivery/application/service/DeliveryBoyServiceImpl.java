package com.bazaarhub.backend.delivery.application.service;

import com.bazaarhub.backend.delivery.application.dto.request.*;
import com.bazaarhub.backend.delivery.application.dto.response.DeliveryBoyResponse;
import com.bazaarhub.backend.delivery.application.mapper.DeliveryMapper;
import com.bazaarhub.backend.delivery.domain.exception.DeliveryBoyAlreadyExistsException;
import com.bazaarhub.backend.delivery.domain.exception.DeliveryBoyNotFoundException;
import com.bazaarhub.backend.delivery.domain.exception.InvalidDeliveryOperationException;
import com.bazaarhub.backend.delivery.domain.model.DeliveryBoyAvailabilityStatus;
import com.bazaarhub.backend.delivery.domain.model.DeliveryBoyVerificationStatus;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryBoyEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataDeliveryBoyRepository;
import com.bazaarhub.backend.identity.domain.exception.UserNotFoundException;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.RoleEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataRoleRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class DeliveryBoyServiceImpl implements DeliveryBoyService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryBoyServiceImpl.class);

    private final SpringDataDeliveryBoyRepository deliveryBoyRepository;
    private final SpringDataUserRepository userRepository;
    private final SpringDataRoleRepository roleRepository;
    private final DeliveryMapper deliveryMapper;

    public DeliveryBoyServiceImpl(SpringDataDeliveryBoyRepository deliveryBoyRepository,
                                  SpringDataUserRepository userRepository,
                                  SpringDataRoleRepository roleRepository,
                                  DeliveryMapper deliveryMapper) {
        this.deliveryBoyRepository = deliveryBoyRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.deliveryMapper = deliveryMapper;
    }

    @Override
    @Transactional
    public DeliveryBoyResponse onboard(Long userId, DeliveryBoyOnboardRequest request) {
        if (deliveryBoyRepository.existsByUserId(userId)) {
            throw new DeliveryBoyAlreadyExistsException("User is already registered as a delivery partner");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        // Assign ROLE_DELIVERY to the user if not already present
        roleRepository.findByName("ROLE_DELIVERY").ifPresent(role -> {
            if (!user.getRoles().contains(role)) {
                user.getRoles().add(role);
                userRepository.save(user);
                log.info("[DELIVERY ONBOARD] Granted ROLE_DELIVERY to user ID: {}", userId);
            }
        });

        DeliveryBoyEntity entity = DeliveryBoyEntity.builder()
                .user(user)
                .vehicleType(request.getVehicleType())
                .vehicleNumber(request.getVehicleNumber())
                .verificationStatus(DeliveryBoyVerificationStatus.PENDING)
                .availabilityStatus(DeliveryBoyAvailabilityStatus.OFFLINE)
                .createdBy(user)
                .updatedBy(user)
                .build();

        DeliveryBoyEntity saved = deliveryBoyRepository.save(entity);
        log.info("[DELIVERY ONBOARD] Successfully onboarded delivery partner ID: {} for user ID: {}", saved.getId(), userId);

        return deliveryMapper.toDeliveryBoyResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryBoyResponse getProfile(Long userId) {
        DeliveryBoyEntity entity = deliveryBoyRepository.findByUserId(userId)
                .orElseThrow(() -> new DeliveryBoyNotFoundException("Delivery partner profile not found for user ID: " + userId));
        return deliveryMapper.toDeliveryBoyResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryBoyResponse getDeliveryBoyById(Long deliveryBoyId) {
        DeliveryBoyEntity entity = deliveryBoyRepository.findById(deliveryBoyId)
                .orElseThrow(() -> new DeliveryBoyNotFoundException(deliveryBoyId));
        return deliveryMapper.toDeliveryBoyResponse(entity);
    }

    @Override
    @Transactional
    public DeliveryBoyResponse updateProfile(Long userId, DeliveryBoyUpdateRequest request) {
        DeliveryBoyEntity entity = deliveryBoyRepository.findByUserId(userId)
                .orElseThrow(() -> new DeliveryBoyNotFoundException("Delivery partner profile not found for user ID: " + userId));

        if (request.getVehicleType() != null) {
            entity.setVehicleType(request.getVehicleType());
        }
        if (request.getVehicleNumber() != null && !request.getVehicleNumber().isBlank()) {
            entity.setVehicleNumber(request.getVehicleNumber());
        }

        entity.setUpdatedBy(entity.getUser());
        entity.setUpdatedAt(Instant.now());

        DeliveryBoyEntity updated = deliveryBoyRepository.save(entity);
        log.info("[DELIVERY UPDATE] Updated vehicle details for delivery boy ID: {}", updated.getId());

        return deliveryMapper.toDeliveryBoyResponse(updated);
    }

    @Override
    @Transactional
    public DeliveryBoyResponse updateAvailability(Long userId, UpdateAvailabilityRequest request) {
        DeliveryBoyEntity entity = deliveryBoyRepository.findByUserId(userId)
                .orElseThrow(() -> new DeliveryBoyNotFoundException("Delivery partner profile not found for user ID: " + userId));

        if (entity.getAvailabilityStatus() == DeliveryBoyAvailabilityStatus.SUSPENDED) {
            throw new InvalidDeliveryOperationException("Account is suspended. You cannot change availability status.");
        }

        if (request.getAvailabilityStatus() == DeliveryBoyAvailabilityStatus.AVAILABLE &&
            entity.getVerificationStatus() != DeliveryBoyVerificationStatus.VERIFIED) {
            throw new InvalidDeliveryOperationException("Profile KYC is not verified. Only verified partners can go online.");
        }

        // Only toggle between AVAILABLE and OFFLINE
        if (request.getAvailabilityStatus() != DeliveryBoyAvailabilityStatus.AVAILABLE &&
            request.getAvailabilityStatus() != DeliveryBoyAvailabilityStatus.OFFLINE) {
            throw new InvalidDeliveryOperationException("Delivery partners can only toggle availability between AVAILABLE and OFFLINE.");
        }

        entity.setAvailabilityStatus(request.getAvailabilityStatus());
        entity.setUpdatedBy(entity.getUser());
        entity.setUpdatedAt(Instant.now());

        DeliveryBoyEntity saved = deliveryBoyRepository.save(entity);
        log.info("[DELIVERY AVAILABILITY] Delivery boy ID {} availability updated to: {}", saved.getId(), saved.getAvailabilityStatus());

        return deliveryMapper.toDeliveryBoyResponse(saved);
    }

    @Override
    @Transactional
    public DeliveryBoyResponse adminVerifyDeliveryBoy(Long adminId, Long deliveryBoyId, AdminVerifyDeliveryBoyRequest request) {
        DeliveryBoyEntity entity = deliveryBoyRepository.findById(deliveryBoyId)
                .orElseThrow(() -> new DeliveryBoyNotFoundException(deliveryBoyId));

        UserEntity admin = userRepository.findById(adminId)
                .orElseThrow(() -> new UserNotFoundException("Admin user not found"));

        entity.setVerificationStatus(request.getVerificationStatus());
        entity.setVerifiedBy(admin);
        entity.setVerifiedAt(Instant.now());
        entity.setUpdatedBy(admin);
        entity.setUpdatedAt(Instant.now());

        if (request.getVerificationStatus() == DeliveryBoyVerificationStatus.VERIFIED) {
            if (entity.getAvailabilityStatus() == DeliveryBoyAvailabilityStatus.SUSPENDED) {
                entity.setAvailabilityStatus(DeliveryBoyAvailabilityStatus.OFFLINE);
            }
        } else if (request.getVerificationStatus() == DeliveryBoyVerificationStatus.REJECTED) {
            entity.setAvailabilityStatus(DeliveryBoyAvailabilityStatus.OFFLINE);
        }

        DeliveryBoyEntity saved = deliveryBoyRepository.save(entity);
        log.info("[ADMIN VERIFY] Delivery boy ID {} KYC status set to {} by admin ID: {}",
                deliveryBoyId, request.getVerificationStatus(), adminId);

        return deliveryMapper.toDeliveryBoyResponse(saved);
    }

    @Override
    @Transactional
    public DeliveryBoyResponse adminUpdateStatus(Long adminId, Long deliveryBoyId, AdminUpdateDeliveryBoyStatusRequest request) {
        DeliveryBoyEntity entity = deliveryBoyRepository.findById(deliveryBoyId)
                .orElseThrow(() -> new DeliveryBoyNotFoundException(deliveryBoyId));

        UserEntity admin = userRepository.findById(adminId)
                .orElseThrow(() -> new UserNotFoundException("Admin user not found"));

        entity.setAvailabilityStatus(request.getAvailabilityStatus());
        entity.setUpdatedBy(admin);
        entity.setUpdatedAt(Instant.now());

        DeliveryBoyEntity saved = deliveryBoyRepository.save(entity);
        log.info("[ADMIN STATUS] Delivery boy ID {} status updated to: {} by admin ID: {}",
                deliveryBoyId, request.getAvailabilityStatus(), adminId);

        return deliveryMapper.toDeliveryBoyResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryBoyResponse> adminListDeliveryBoys(
            DeliveryBoyVerificationStatus verificationStatus,
            DeliveryBoyAvailabilityStatus availabilityStatus,
            Pageable pageable) {
        return deliveryBoyRepository.searchDeliveryBoys(verificationStatus, availabilityStatus, pageable)
                .map(deliveryMapper::toDeliveryBoyResponse);
    }
}
