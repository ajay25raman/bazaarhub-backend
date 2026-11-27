package com.bazaarhub.backend.delivery.infrastructure.adapter;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserDeviceEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Service managing Firebase Cloud Messaging (FCM) push notifications
 * for delivery lifecycle events dispatched to customers and delivery partners.
 */
@Service
public class FcmPushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(FcmPushNotificationService.class);

    private final SpringDataUserDeviceRepository userDeviceRepository;
    private final HttpClient httpClient;

    @Value("${firebase.fcm.server-key:placeholder_fcm_key}")
    private String serverKey;

    @Value("${firebase.fcm.enabled:false}")
    private boolean fcmEnabled;

    public FcmPushNotificationService(SpringDataUserDeviceRepository userDeviceRepository) {
        this.userDeviceRepository = userDeviceRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Sends a push notification for a specific delivery event to all active devices of the recipient user.
     *
     * @param userId Target recipient user ID
     * @param eventType Delivery event type (e.g. DELIVERY_ASSIGNED, OUT_FOR_DELIVERY, DELIVERED)
     * @param title Notification title
     * @param message Notification body
     * @param shipmentId Associated shipment ID
     */
    public void sendDeliveryNotification(Long userId, String eventType, String title, String message, Long shipmentId) {
        log.info("[DELIVERY NOTIFICATION] Recipient: {}, Event: {}, Title: '{}', Shipment: {}",
                userId, eventType, title, shipmentId);

        List<UserDeviceEntity> devices = userDeviceRepository.findAllByUserIdAndActiveTrue(userId);
        if (devices.isEmpty()) {
            log.debug("[FCM SKIP] No active user devices registered for user ID: {}", userId);
            return;
        }

        for (UserDeviceEntity device : devices) {
            String token = device.getPushToken();
            if (token != null && !token.isBlank()) {
                dispatchToFcm(token, eventType, title, message, shipmentId);
            }
        }
    }

    private void dispatchToFcm(String pushToken, String eventType, String title, String body, Long shipmentId) {
        if (!fcmEnabled || "placeholder_fcm_key".equals(serverKey)) {
            log.debug("[FCM SIMULATED] Push to token {}: [{}] {} - {}",
                    pushToken.substring(0, Math.min(10, pushToken.length())) + "...", eventType, title, body);
            return;
        }

        try {
            String payload = String.format("""
                    {
                      "to": "%s",
                      "notification": {
                        "title": "%s",
                        "body": "%s"
                      },
                      "data": {
                        "eventType": "%s",
                        "shipmentId": "%s"
                      }
                    }
                    """, pushToken, title, body, eventType, shipmentId != null ? shipmentId.toString() : "");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://fcm.googleapis.com/fcm/send"))
                    .timeout(Duration.ofSeconds(6))
                    .header("Authorization", "key=" + serverKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.debug("[FCM SENT] Successfully dispatched to device");
            } else {
                log.warn("[FCM WARN] FCM dispatch returned status {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception ex) {
            log.warn("[FCM ERROR] Failed to dispatch push notification: {}", ex.getMessage());
        }
    }
}
