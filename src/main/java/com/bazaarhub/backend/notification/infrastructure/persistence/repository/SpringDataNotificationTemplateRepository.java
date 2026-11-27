package com.bazaarhub.backend.notification.infrastructure.persistence.repository;

import com.bazaarhub.backend.notification.domain.model.NotificationChannel;
import com.bazaarhub.backend.notification.domain.model.TemplateStatus;
import com.bazaarhub.backend.notification.infrastructure.persistence.entity.NotificationTemplateEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataNotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, Long> {

    Optional<NotificationTemplateEntity> findByTemplateKeyAndChannel(String templateKey, NotificationChannel channel);

    boolean existsByTemplateKeyAndChannel(String templateKey, NotificationChannel channel);

    Page<NotificationTemplateEntity> findByStatus(TemplateStatus status, Pageable pageable);
}
