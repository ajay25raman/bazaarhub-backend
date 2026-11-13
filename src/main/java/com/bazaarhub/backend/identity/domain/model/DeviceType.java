package com.bazaarhub.backend.identity.domain.model;

/**
 * Device type matching the MariaDB `user_devices.device_type` ENUM definition:
 * enum('ANDROID','IOS','WEB')
 */
public enum DeviceType {
    ANDROID,
    IOS,
    WEB
}
