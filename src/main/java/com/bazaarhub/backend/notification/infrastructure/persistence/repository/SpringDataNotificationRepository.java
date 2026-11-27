package com.bazaarhub.backend.notification.infrastructure.persistence.repository;

import com.bazaarhub.backend.notification.domain.model.DeliveryStatus;
import com.bazaarhub.backend.notification.domain.model.NotificationChannel;
import com.bazaarhub.backend.notification.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface SpringDataNotificationRepository extends JpaRepository<NotificationEntity, Long> {

    Page<NotificationEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndReadFalse(Long userId);

    Optional<NotificationEntity> findByIdAndUserId(Long id, Long userId);

    boolean existsByDeduplicationKey(String deduplicationKey);

    Optional<NotificationEntity> findByDeduplicationKey(String deduplicationKey);

    @Modifying
    @Query("""
        UPDATE NotificationEntity n 
        SET n.read = true, n.readAt = :readAt, n.deliveryStatus = com.bazaarhub.backend.notification.domain.model.DeliveryStatus.READ 
        WHERE n.user.id = :userId AND n.read = false
    """)
    int markAllAsReadForUser(@Param("userId") Long userId, @Param("readAt") Instant readAt);

    @Query("""
        SELECT n FROM NotificationEntity n
        WHERE (:userId IS NULL OR n.user.id = :userId)
          AND (:channel IS NULL OR n.channel = :channel)
          AND (:status IS NULL OR n.deliveryStatus = :status)
        ORDER BY n.createdAt DESC
    """)
    Page<NotificationEntity> findByFilters(
            @Param("userId") Long userId,
            @Param("channel") NotificationChannel channel,
            @Param("status") DeliveryStatus status,
            Pageable pageable
    );
}
