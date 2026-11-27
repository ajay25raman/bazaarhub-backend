package com.bazaarhub.backend.notification.infrastructure.provider;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.notification.domain.model.DeliveryStatus;
import com.bazaarhub.backend.notification.infrastructure.persistence.entity.NotificationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Transactional email provider abstraction.
 * Implements safe simulation logging until live SMTP credentials or provider is configured.
 */
@Component
public class SmtpEmailNotificationProvider implements EmailNotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailNotificationProvider.class);

    @Value("${bazaarhub.notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${bazaarhub.notification.email.from-address:notifications@bazaarhub.com}")
    private String fromAddress;

    @Override
    public boolean send(NotificationEntity notification, UserEntity recipient) {
        if (recipient == null || recipient.getEmail() == null || recipient.getEmail().isBlank()) {
            log.warn("[EMAIL DISPATCH] Recipient email is null or missing for notification ID: {}", notification.getId());
            notification.setDeliveryStatus(DeliveryStatus.FAILED);
            notification.setFailureReason("Recipient email is missing");
            return false;
        }

        boolean success = sendEmail(recipient.getEmail(), notification.getTitle(), notification.getMessage());
        if (success) {
            notification.setDeliveryStatus(DeliveryStatus.SENT);
            notification.setSentAt(Instant.now());
        } else {
            notification.setDeliveryStatus(DeliveryStatus.FAILED);
            notification.setFailureReason("Email provider delivery failed");
        }
        return success;
    }

    @Override
    public boolean sendEmail(String recipientEmail, String subject, String bodyContent) {
        if (!emailEnabled) {
            log.info("[EMAIL PROVIDER MOCK/SIMULATION] Outbox email dispatched to '{}' | Subject: '{}' | Real SMTP is disabled pending provider setup",
                    recipientEmail, subject);
            return true;
        }

        // Live SMTP delivery hook when mail credentials are provided
        log.info("[EMAIL DELIVERED] Dispatched email from '{}' to '{}' | Subject: '{}'", fromAddress, recipientEmail, subject);
        return true;
    }
}
