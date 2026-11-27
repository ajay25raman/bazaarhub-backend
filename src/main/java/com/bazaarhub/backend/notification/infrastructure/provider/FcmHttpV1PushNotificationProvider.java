package com.bazaarhub.backend.notification.infrastructure.provider;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserDeviceEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserDeviceRepository;
import com.bazaarhub.backend.notification.domain.model.DeliveryStatus;
import com.bazaarhub.backend.notification.infrastructure.persistence.entity.NotificationEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FCM Push Notification Provider implementing modern Firebase Cloud Messaging HTTP v1 API.
 * Uses OAuth2 Bearer token authentication rather than deprecated legacy server keys.
 */
@Component
public class FcmHttpV1PushNotificationProvider implements PushNotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(FcmHttpV1PushNotificationProvider.class);

    @Value("${firebase.fcm.enabled:false}")
    private boolean fcmEnabled;

    @Value("${firebase.fcm.project-id:bazaarhub-marketplace}")
    private String fcmProjectId;

    @Value("${firebase.fcm.access-token:placeholder_oauth_token}")
    private String fcmAccessToken;

    private final SpringDataUserDeviceRepository userDeviceRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public FcmHttpV1PushNotificationProvider(SpringDataUserDeviceRepository userDeviceRepository,
                                             ObjectMapper objectMapper) {
        this.userDeviceRepository = userDeviceRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public boolean send(NotificationEntity notification, UserEntity recipient) {
        if (recipient == null || recipient.getId() == null) {
            log.warn("[FCM PUSH] Cannot send push notification: recipient user is null");
            notification.setDeliveryStatus(DeliveryStatus.FAILED);
            notification.setFailureReason("Recipient user is missing or invalid");
            return false;
        }

        List<UserDeviceEntity> activeDevices = userDeviceRepository.findAllByUserIdAndActiveTrue(recipient.getId());
        if (activeDevices.isEmpty()) {
            log.info("[FCM PUSH] User ID {} has no active registered push devices", recipient.getId());
            notification.setDeliveryStatus(DeliveryStatus.SENT);
            notification.setSentAt(Instant.now());
            return true;
        }

        boolean anySuccess = false;
        for (UserDeviceEntity device : activeDevices) {
            String token = device.getPushToken();
            if (token == null || token.isBlank()) {
                continue;
            }

            boolean sent = sendPush(token, notification.getTitle(), notification.getMessage(), notification);
            if (sent) {
                anySuccess = true;
            }
        }

        if (anySuccess) {
            notification.setDeliveryStatus(DeliveryStatus.DELIVERED);
            notification.setSentAt(Instant.now());
        } else {
            notification.setDeliveryStatus(DeliveryStatus.SENT);
            notification.setSentAt(Instant.now());
        }
        return true;
    }

    @Override
    public boolean sendPush(String targetToken, String title, String body, NotificationEntity notification) {
        if (!fcmEnabled || "placeholder_oauth_token".equals(fcmAccessToken)) {
            log.info("[FCM HTTP v1 SIMULATION] Push sent to token '{}...': title='{}'",
                    targetToken.length() > 8 ? targetToken.substring(0, 8) : targetToken, title);
            return true;
        }

        try {
            Map<String, Object> messagePayload = new HashMap<>();
            Map<String, String> notificationMap = new HashMap<>();
            notificationMap.put("title", title);
            notificationMap.put("body", body);

            Map<String, String> dataMap = new HashMap<>();
            if (notification != null) {
                dataMap.put("notificationId", String.valueOf(notification.getId()));
                dataMap.put("notificationType", notification.getNotificationType());
                if (notification.getReferenceId() != null) {
                    dataMap.put("referenceId", String.valueOf(notification.getReferenceId()));
                }
                if (notification.getReferenceType() != null) {
                    dataMap.put("referenceType", notification.getReferenceType());
                }
            }

            Map<String, Object> innerMessage = new HashMap<>();
            innerMessage.put("token", targetToken);
            innerMessage.put("notification", notificationMap);
            innerMessage.put("data", dataMap);

            messagePayload.put("message", innerMessage);

            String requestBody = objectMapper.writeValueAsString(messagePayload);
            String url = String.format("https://fcm.googleapis.com/v1/projects/%s/messages:send", fcmProjectId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json; UTF-8")
                    .header("Authorization", "Bearer " + fcmAccessToken)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("[FCM HTTP v1 SUCCESS] Push delivered to token '{}...'", targetToken.substring(0, Math.min(8, targetToken.length())));
                return true;
            } else {
                log.warn("[FCM HTTP v1 ERROR] Status {}: {}", response.statusCode(), response.body());
                if (response.body() != null && (response.body().contains("UNREGISTERED") || response.body().contains("NOT_FOUND"))) {
                    deactivateToken(targetToken);
                }
                return false;
            }
        } catch (Exception e) {
            log.error("[FCM HTTP v1 FAILURE] Exception during push dispatch: {}", e.getMessage());
            return false;
        }
    }

    private void deactivateToken(String token) {
        try {
            userDeviceRepository.findAll().stream()
                    .filter(d -> token.equals(d.getPushToken()))
                    .forEach(d -> {
                        d.setActive(false);
                        userDeviceRepository.save(d);
                        log.info("[FCM DEVICE DEACTIVATED] Expired token deactivated for device ID: {}", d.getDeviceId());
                    });
        } catch (Exception ex) {
            log.warn("[FCM CLEANUP] Could not deactivate invalid token: {}", ex.getMessage());
        }
    }
}
