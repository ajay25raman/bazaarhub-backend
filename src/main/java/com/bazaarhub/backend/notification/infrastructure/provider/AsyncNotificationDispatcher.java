package com.bazaarhub.backend.notification.infrastructure.provider;

import com.bazaarhub.backend.notification.domain.model.DeliveryStatus;
import com.bazaarhub.backend.notification.infrastructure.persistence.entity.NotificationEntity;
import com.bazaarhub.backend.notification.infrastructure.persistence.repository.SpringDataNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Asynchronous notification dispatcher with bounded retries and safe isolation.
 * Runs in a separate thread and new transaction to guarantee zero rollback impact
 * on originating business operations.
 */
@Component
public class AsyncNotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AsyncNotificationDispatcher.class);
    private static final int MAX_RETRIES = 2;

    private final SpringDataNotificationRepository notificationRepository;
    private final Map<String, NotificationChannelProvider> channelProviders;

    public AsyncNotificationDispatcher(SpringDataNotificationRepository notificationRepository,
                                       List<NotificationChannelProvider> providers) {
        this.notificationRepository = notificationRepository;
        this.channelProviders = providers.stream()
                .collect(Collectors.toMap(p -> p.getChannel().name(), p -> p));
    }

    @Async("notificationTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatch(Long notificationId) {
        if (notificationId == null) {
            return;
        }

        NotificationEntity notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null) {
            log.warn("[ASYNC NOTIFICATION] Notification ID {} not found for dispatch", notificationId);
            return;
        }

        NotificationChannelProvider provider = channelProviders.get(notification.getChannel().name());
        if (provider == null) {
            log.warn("[ASYNC NOTIFICATION] No provider registered for channel {}", notification.getChannel());
            notification.setDeliveryStatus(DeliveryStatus.FAILED);
            notification.setFailureReason("No provider for channel: " + notification.getChannel());
            notificationRepository.save(notification);
            return;
        }

        boolean success = false;
        String lastError = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                success = provider.send(notification, notification.getUser());
                if (success) {
                    break;
                } else {
                    lastError = notification.getFailureReason() != null 
                            ? notification.getFailureReason() 
                            : "Provider returned false on attempt " + attempt;
                }
            } catch (Exception ex) {
                lastError = ex.getMessage();
                log.warn("[NOTIFICATION RETRY] Attempt {} failed for ID {}: {}", attempt, notificationId, ex.getMessage());
            }

            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(100L * attempt);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (!success) {
            notification.setDeliveryStatus(DeliveryStatus.FAILED);
            notification.setFailureReason(lastError != null ? lastError : "Failed after " + MAX_RETRIES + " attempts");
        } else {
            if (notification.getDeliveryStatus() == DeliveryStatus.PENDING) {
                notification.setDeliveryStatus(DeliveryStatus.SENT);
            }
            if (notification.getSentAt() == null) {
                notification.setSentAt(Instant.now());
            }
        }

        notificationRepository.save(notification);
        log.info("[ASYNC NOTIFICATION COMPLETE] ID: {}, Channel: {}, Status: {}",
                notification.getId(), notification.getChannel(), notification.getDeliveryStatus());
    }
}
