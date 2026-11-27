package com.bazaarhub.backend.notification.application.service;

import com.bazaarhub.backend.notification.application.dto.request.NotificationPreferenceRequest;
import com.bazaarhub.backend.notification.application.dto.request.SendNotificationRequest;
import com.bazaarhub.backend.notification.application.dto.response.NotificationPreferenceResponse;
import com.bazaarhub.backend.notification.application.dto.response.NotificationResponse;
import com.bazaarhub.backend.notification.application.dto.response.UnreadNotificationCountResponse;
import com.bazaarhub.backend.notification.domain.model.DeliveryStatus;
import com.bazaarhub.backend.notification.domain.model.NotificationChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    NotificationResponse sendNotification(SendNotificationRequest request);

    void dispatchAsync(Long notificationId);

    Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable);

    UnreadNotificationCountResponse getUnreadCount(Long userId);

    NotificationResponse markAsRead(Long notificationId, Long userId);

    int markAllAsRead(Long userId);

    Page<NotificationResponse> getAllNotificationsAdmin(NotificationChannel channel, DeliveryStatus status, Long userId, Pageable pageable);

    NotificationPreferenceResponse getNotificationPreferences(Long userId);

    NotificationPreferenceResponse updateNotificationPreferences(Long userId, NotificationPreferenceRequest request);
}
