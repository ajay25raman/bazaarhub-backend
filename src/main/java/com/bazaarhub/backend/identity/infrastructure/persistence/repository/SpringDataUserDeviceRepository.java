package com.bazaarhub.backend.identity.infrastructure.persistence.repository;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserDeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataUserDeviceRepository extends JpaRepository<UserDeviceEntity, Long> {

    Optional<UserDeviceEntity> findByDeviceId(String deviceId);

    Optional<UserDeviceEntity> findByUserIdAndDeviceId(Long userId, String deviceId);

    List<UserDeviceEntity> findAllByUserId(Long userId);

    List<UserDeviceEntity> findAllByUserIdAndActiveTrue(Long userId);
}
