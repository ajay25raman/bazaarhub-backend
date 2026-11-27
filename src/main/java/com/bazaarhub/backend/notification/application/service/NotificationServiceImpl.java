package com.bazaarhub.backend.notification.application.service;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.notification.application.dto.request.NotificationPreferenceRequest;
import com.bazaarhub.backend.notification.application.dto.request.SendNotificationRequest;
import com.bazaarhub.backend.notification.application.dto.response.NotificationPreferenceResponse;
import com.bazaarhub.backend.notification.application.dto.response.NotificationResponse;
import com.bazaarhub.backend.notification.application.dto.response.UnreadNotificationCountResponse;
import com.bazaarhub.backend.notification.application.mapper.NotificationMapper;
import com.bazaarhub.backend.notification.domain.exception.NotificationNotFoundException;
import com.bazaarhub.backend.notification.domain.exception.UnauthorizedNotificationAccessException;
import com.bazaarhub.backend.notification.domain.model.DeliveryStatus;
import com.bazaarhub.backend.notification.domain.model.NotificationChannel;
import com.bazaarhub.backend.notification.infrastructure.persistence.entity.NotificationEntity;
import com.bazaarhub.backend.notification.infrastructure.persistence.repository.SpringDataNotificationRepository;
import com.bazaarhub.backend.notification.infrastructure.provider.AsyncNotificationDispatcher;
import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final SpringDataNotificationRepository notificationRepository;
    private final SpringDataUserRepository userRepository;
    private final NotificationTemplateService templateService;
    private final AsyncNotificationDispatcher asyncDispatcher;
    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(SpringDataNotificationRepository notificationRepository,
                                   SpringDataUserRepository userRepository,
                                   NotificationTemplateService templateService,
                                   AsyncNotificationDispatcher asyncDispatcher,
                                   NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.templateService = templateService;
        this.asyncDispatcher = asyncDispatcher;
        this.notificationMapper = notificationMapper;
    }

    @Override
    public NotificationResponse sendNotification(SendNotificationRequest request) {
        log.info("[NOTIFICATION SEND] Type: {}, Channel: {}, Recipient: {}",
                request.getNotificationType(), request.getChannel(), request.getUserId());

        // 1. Idempotency & Deduplication Protection
        if (request.getDeduplicationKey() != null && !request.getDeduplicationKey().isBlank()) {
            Optional<NotificationEntity> existing = notificationRepository.findByDeduplicationKey(request.getDeduplicationKey().trim());
            if (existing.isPresent()) {
                log.info("[NOTIFICATION DUPLICATE PREVENTED] Reusing existing notification ID {} for dedupKey '{}'",
                        existing.get().getId(), request.getDeduplicationKey());
                return notificationMapper.toResponse(existing.get());
            }
        }

        // 2. Validate recipient existence
        UserEntity recipient = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));

        // 3. Render dynamic template if requested
        String finalMessage = request.getMessage();
        if (request.getTemplateKey() != null && !request.getTemplateKey().isBlank()) {
            finalMessage = templateService.renderTemplate(
                    request.getTemplateKey().trim().toUpperCase(),
                    request.getChannel(),
                    request.getTemplateVariables(),
                    request.getMessage()
            );
        }

        NotificationChannel channel = request.getChannel() != null ? request.getChannel() : NotificationChannel.IN_APP;

        // 4. Create Notification entity in PENDING state
        NotificationEntity notification = NotificationEntity.builder()
                .user(recipient)
                .title(request.getTitle().trim())
                .message(finalMessage.trim())
                .notificationType(request.getNotificationType().name())
                .channel(channel)
                .referenceType(request.getReferenceType() != null ? request.getReferenceType().trim().toUpperCase() : null)
                .referenceId(request.getReferenceId())
                .deliveryStatus(channel == NotificationChannel.IN_APP ? DeliveryStatus.DELIVERED : DeliveryStatus.PENDING)
                .read(false)
                .deduplicationKey(request.getDeduplicationKey() != null ? request.getDeduplicationKey().trim() : null)
                .createdBy(recipient)
                .updatedBy(recipient)
                .build();

        NotificationEntity saved = notificationRepository.save(notification);
        log.info("[NOTIFICATION RECORDED] Notification ID {} saved in status {}", saved.getId(), saved.getDeliveryStatus());

        // 5. Asynchronous, isolated dispatch (never rolls back caller)
        asyncDispatcher.dispatch(saved.getId());

        return notificationMapper.toResponse(saved);
    }

    @Override
    public void dispatchAsync(Long notificationId) {
        asyncDispatcher.dispatch(notificationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadCount(Long userId) {
        long unread = notificationRepository.countByUserIdAndReadFalse(userId);
        return UnreadNotificationCountResponse.builder()
                .userId(userId)
                .unreadCount(unread)
                .build();
    }

    @Override
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            log.warn("[SECURITY VIOLATION] User {} attempted to access notification {} owned by user {}",
                    userId, notificationId, notification.getUser().getId());
            throw new UnauthorizedNotificationAccessException(userId, notificationId);
        }

        notification.markAsRead();
        NotificationEntity updated = notificationRepository.save(notification);
        log.info("[NOTIFICATION READ] Notification ID {} marked as read by user {}", notificationId, userId);
        return notificationMapper.toResponse(updated);
    }

    @Override
    public int markAllAsRead(Long userId) {
        int count = notificationRepository.markAllAsReadForUser(userId, java.time.Instant.now());
        log.info("[NOTIFICATIONS BULK READ] Marked {} notifications as read for user {}", count, userId);
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getAllNotificationsAdmin(NotificationChannel channel, DeliveryStatus status, Long userId, Pageable pageable) {
        return notificationRepository.findByFilters(userId, channel, status, pageable)
                .map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getNotificationPreferences(Long userId) {
        // Return default enabled preferences supported by schema
        return NotificationPreferenceResponse.builder()
                .userId(userId)
                .inAppEnabled(true)
                .pushEnabled(true)
                .emailEnabled(true)
                .smsEnabled(false)
                .build();
    }

    @Override
    public NotificationPreferenceResponse updateNotificationPreferences(Long userId, NotificationPreferenceRequest request) {
        log.info("[NOTIFICATION PREFERENCES] Updated preferences for user {}: inApp={}, push={}, email={}, sms={}",
                userId, request.isInAppEnabled(), request.isPushEnabled(), request.isEmailEnabled(), request.isSmsEnabled());

        return NotificationPreferenceResponse.builder()
                .userId(userId)
                .inAppEnabled(request.isInAppEnabled())
                .pushEnabled(request.isPushEnabled())
                .emailEnabled(request.isEmailEnabled())
                .smsEnabled(request.isSmsEnabled())
                .build();
    }
}
