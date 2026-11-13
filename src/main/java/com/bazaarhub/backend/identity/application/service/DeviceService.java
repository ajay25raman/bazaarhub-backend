package com.bazaarhub.backend.identity.application.service;

import com.bazaarhub.backend.identity.application.dto.DeviceInfoRequest;
import com.bazaarhub.backend.identity.domain.model.DeviceType;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserDeviceEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service managing user device sessions and hardware associations strictly mapped to `user_devices`.
 */
@Service
public class DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);
    private final SpringDataUserDeviceRepository userDeviceRepository;

    public DeviceService(SpringDataUserDeviceRepository userDeviceRepository) {
        this.userDeviceRepository = userDeviceRepository;
    }

    @Transactional
    public void registerOrUpdateDevice(UserEntity user, DeviceInfoRequest deviceInfo) {
        if (deviceInfo == null || deviceInfo.getDeviceId() == null || deviceInfo.getDeviceId().isBlank()) {
            return;
        }

        String deviceId = deviceInfo.getDeviceId().trim();
        DeviceType deviceType = deviceInfo.getDeviceType() != null ? deviceInfo.getDeviceType() : DeviceType.WEB;

        Optional<UserDeviceEntity> existingDevice = userDeviceRepository.findByDeviceId(deviceId);

        if (existingDevice.isPresent()) {
            UserDeviceEntity device = existingDevice.get();
            device.setUser(user);
            if (deviceInfo.getDeviceName() != null) {
                device.setDeviceName(deviceInfo.getDeviceName());
            }
            device.setDeviceType(deviceType);
            if (deviceInfo.getPushToken() != null) {
                device.setPushToken(deviceInfo.getPushToken());
            }
            device.setActive(true);
            device.setLastSeenAt(Instant.now());
            device.setUpdatedBy(user.getId());
            userDeviceRepository.save(device);
            log.info("[DEVICE SERVICE] Updated active session for device ID '{}' (user ID: {})", deviceId, user.getId());
        } else {
            UserDeviceEntity newDevice = UserDeviceEntity.builder()
                    .user(user)
                    .deviceId(deviceId)
                    .deviceName(deviceInfo.getDeviceName())
                    .deviceType(deviceType)
                    .pushToken(deviceInfo.getPushToken())
                    .active(true)
                    .lastSeenAt(Instant.now())
                    .createdBy(user.getId())
                    .build();
            userDeviceRepository.save(newDevice);
            log.info("[DEVICE SERVICE] Registered new active device ID '{}' (user ID: {})", deviceId, user.getId());
        }
    }

    @Transactional(readOnly = true)
    public boolean isDeviceActive(Long userId, String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return true;
        }
        return userDeviceRepository.findByUserIdAndDeviceId(userId, deviceId.trim())
                .map(UserDeviceEntity::isActive)
                .orElse(true);
    }

    @Transactional
    public void deactivateDevice(Long userId, String deviceId) {
        if (deviceId != null && !deviceId.isBlank()) {
            userDeviceRepository.findByUserIdAndDeviceId(userId, deviceId.trim())
                    .ifPresent(device -> {
                        device.setActive(false);
                        device.setUpdatedBy(userId);
                        userDeviceRepository.save(device);
                        log.info("[DEVICE SERVICE] Deactivated device ID '{}' for user ID: {}", deviceId, userId);
                    });
        } else {
            List<UserDeviceEntity> activeDevices = userDeviceRepository.findAllByUserIdAndActiveTrue(userId);
            for (UserDeviceEntity device : activeDevices) {
                device.setActive(false);
                device.setUpdatedBy(userId);
            }
            userDeviceRepository.saveAll(activeDevices);
            log.info("[DEVICE SERVICE] Deactivated {} active device sessions for user ID: {}", activeDevices.size(), userId);
        }
    }
}
