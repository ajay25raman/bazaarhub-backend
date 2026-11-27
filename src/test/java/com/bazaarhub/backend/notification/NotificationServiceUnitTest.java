package com.bazaarhub.backend.notification;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.notification.application.dto.request.NotificationPreferenceRequest;
import com.bazaarhub.backend.notification.application.dto.request.SendNotificationRequest;
import com.bazaarhub.backend.notification.application.dto.response.NotificationPreferenceResponse;
import com.bazaarhub.backend.notification.application.dto.response.NotificationResponse;
import com.bazaarhub.backend.notification.application.dto.response.UnreadNotificationCountResponse;
import com.bazaarhub.backend.notification.application.mapper.NotificationMapper;
import com.bazaarhub.backend.notification.application.service.NotificationServiceImpl;
import com.bazaarhub.backend.notification.application.service.NotificationTemplateService;
import com.bazaarhub.backend.notification.domain.exception.NotificationNotFoundException;
import com.bazaarhub.backend.notification.domain.exception.UnauthorizedNotificationAccessException;
import com.bazaarhub.backend.notification.domain.model.DeliveryStatus;
import com.bazaarhub.backend.notification.domain.model.NotificationChannel;
import com.bazaarhub.backend.notification.domain.model.NotificationType;
import com.bazaarhub.backend.notification.infrastructure.persistence.entity.NotificationEntity;
import com.bazaarhub.backend.notification.infrastructure.persistence.repository.SpringDataNotificationRepository;
import com.bazaarhub.backend.notification.infrastructure.provider.AsyncNotificationDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceUnitTest {

    @Mock
    private SpringDataNotificationRepository notificationRepository;

    @Mock
    private SpringDataUserRepository userRepository;

    @Mock
    private NotificationTemplateService templateService;

    @Mock
    private AsyncNotificationDispatcher asyncDispatcher;

    @Spy
    private NotificationMapper notificationMapper = new NotificationMapper();

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UserEntity user;
    private UserEntity otherUser;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder()
                .id(100L)
                .email("customer@example.com")
                .phone("+919876543210")
                .fullName("John Doe")
                .build();

        otherUser = UserEntity.builder()
                .id(200L)
                .email("other@example.com")
                .phone("+919876543211")
                .fullName("Jane Smith")
                .build();
    }

    @Test
    @DisplayName("Send in-app notification: Success with async dispatch")
    void testSendNotification_InApp_Success() {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId(100L)
                .channel(NotificationChannel.IN_APP)
                .notificationType(NotificationType.ORDER_PLACED)
                .title("Order Confirmed")
                .message("Your order has been placed successfully.")
                .deduplicationKey("ORD-CONF-1234")
                .build();

        when(notificationRepository.findByDeduplicationKey("ORD-CONF-1234")).thenReturn(Optional.empty());
        when(userRepository.findById(100L)).thenReturn(Optional.of(user));

        NotificationEntity savedEntity = NotificationEntity.builder()
                .id(1L)
                .user(user)
                .channel(NotificationChannel.IN_APP)
                .notificationType(NotificationType.ORDER_PLACED.name())
                .title("Order Confirmed")
                .message("Your order has been placed successfully.")
                .deliveryStatus(DeliveryStatus.PENDING)
                .read(false)
                .deduplicationKey("ORD-CONF-1234")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(savedEntity);

        NotificationResponse response = notificationService.sendNotification(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(100L, response.getUserId());
        assertEquals(NotificationChannel.IN_APP, response.getChannel());
        assertEquals("Order Confirmed", response.getTitle());
        assertEquals(DeliveryStatus.PENDING, response.getDeliveryStatus());
        assertFalse(response.isRead());

        verify(notificationRepository).save(any(NotificationEntity.class));
        verify(asyncDispatcher).dispatch(1L);
    }

    @Test
    @DisplayName("Send notification: Deduplication key prevents duplicate insert and dispatch")
    void testSendNotification_Deduplication_SkipsCreation() {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId(100L)
                .channel(NotificationChannel.PUSH)
                .notificationType(NotificationType.ORDER_PLACED)
                .title("Duplicate Test")
                .message("Duplicate Message")
                .deduplicationKey("DEDUP-UNIQUE-KEY-001")
                .build();

        NotificationEntity existing = NotificationEntity.builder()
                .id(50L)
                .user(user)
                .channel(NotificationChannel.PUSH)
                .notificationType(NotificationType.ORDER_PLACED.name())
                .title("Duplicate Test")
                .message("Duplicate Message")
                .deliveryStatus(DeliveryStatus.SENT)
                .read(false)
                .deduplicationKey("DEDUP-UNIQUE-KEY-001")
                .createdAt(Instant.now().minusSeconds(60))
                .updatedAt(Instant.now().minusSeconds(60))
                .build();

        when(notificationRepository.findByDeduplicationKey("DEDUP-UNIQUE-KEY-001")).thenReturn(Optional.of(existing));

        NotificationResponse response = notificationService.sendNotification(request);

        assertNotNull(response);
        assertEquals(50L, response.getId());
        verify(notificationRepository, never()).save(any());
        verify(asyncDispatcher, never()).dispatch(anyLong());
    }

    @Test
    @DisplayName("Send notification with template rendering: Template interpolated successfully")
    void testSendNotification_WithTemplate() {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId(100L)
                .channel(NotificationChannel.EMAIL)
                .notificationType(NotificationType.ORDER_SHIPPED)
                .title("Order Shipped")
                .templateKey("ORDER_SHIPPED_EMAIL")
                .templateVariables(Map.of("trackingNumber", "TRK-999"))
                .message("Fallback shipped message")
                .build();

        when(userRepository.findById(100L)).thenReturn(Optional.of(user));
        when(templateService.renderTemplate(eq("ORDER_SHIPPED_EMAIL"), eq(NotificationChannel.EMAIL), any(), any()))
                .thenReturn("Your order tracking is TRK-999");

        NotificationEntity savedEntity = NotificationEntity.builder()
                .id(2L)
                .user(user)
                .channel(NotificationChannel.EMAIL)
                .notificationType(NotificationType.ORDER_SHIPPED.name())
                .message("Your order tracking is TRK-999")
                .deliveryStatus(DeliveryStatus.PENDING)
                .read(false)
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(savedEntity);

        NotificationResponse response = notificationService.sendNotification(request);

        assertNotNull(response);
        assertEquals("Your order tracking is TRK-999", response.getMessage());
        verify(templateService).renderTemplate(eq("ORDER_SHIPPED_EMAIL"), eq(NotificationChannel.EMAIL), any(), any());
    }

    @Test
    @DisplayName("Get user notifications: Paginated results returned")
    void testGetUserNotifications_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        NotificationEntity entity = NotificationEntity.builder()
                .id(10L)
                .user(user)
                .channel(NotificationChannel.IN_APP)
                .title("Update")
                .message("Your order is packed")
                .read(false)
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(100L, pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

        Page<NotificationResponse> result = notificationService.getUserNotifications(100L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Update", result.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("Get unread notification count: Returns count accurately")
    void testGetUnreadCount_Success() {
        when(notificationRepository.countByUserIdAndReadFalse(100L)).thenReturn(5L);

        UnreadNotificationCountResponse countResponse = notificationService.getUnreadCount(100L);

        assertNotNull(countResponse);
        assertEquals(100L, countResponse.getUserId());
        assertEquals(5L, countResponse.getUnreadCount());
    }

    @Test
    @DisplayName("Mark as read: Success when owned by user")
    void testMarkAsRead_Success() {
        NotificationEntity entity = NotificationEntity.builder()
                .id(10L)
                .user(user)
                .channel(NotificationChannel.IN_APP)
                .read(false)
                .deliveryStatus(DeliveryStatus.SENT)
                .build();

        when(notificationRepository.findById(10L)).thenReturn(Optional.of(entity));
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.markAsRead(10L, 100L);

        assertNotNull(response);
        assertTrue(response.isRead());
        assertEquals(DeliveryStatus.READ, response.getDeliveryStatus());
        assertNotNull(response.getReadAt());
    }

    @Test
    @DisplayName("Mark as read: Throws UnauthorizedNotificationAccessException when owned by another user")
    void testMarkAsRead_Unauthorized_ThrowsException() {
        NotificationEntity entity = NotificationEntity.builder()
                .id(10L)
                .user(otherUser) // Owned by user 200
                .channel(NotificationChannel.IN_APP)
                .read(false)
                .build();

        when(notificationRepository.findById(10L)).thenReturn(Optional.of(entity));

        assertThrows(UnauthorizedNotificationAccessException.class, () ->
                notificationService.markAsRead(10L, 100L)); // Requested by user 100

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Mark as read: Throws NotificationNotFoundException when notification does not exist")
    void testMarkAsRead_NotFound_ThrowsException() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class, () ->
                notificationService.markAsRead(999L, 100L));
    }

    @Test
    @DisplayName("Mark all as read: Successfully invokes repository bulk update")
    void testMarkAllAsRead_Success() {
        when(notificationRepository.markAllAsReadForUser(eq(100L), any(Instant.class))).thenReturn(4);

        int updated = notificationService.markAllAsRead(100L);

        assertEquals(4, updated);
        verify(notificationRepository).markAllAsReadForUser(eq(100L), any(Instant.class));
    }

    @Test
    @DisplayName("Notification preferences: Get and Update behave correctly")
    void testNotificationPreferences() {
        NotificationPreferenceResponse defaultPrefs = notificationService.getNotificationPreferences(100L);
        assertNotNull(defaultPrefs);
        assertTrue(defaultPrefs.isInAppEnabled());
        assertTrue(defaultPrefs.isPushEnabled());
        assertTrue(defaultPrefs.isEmailEnabled());
        assertFalse(defaultPrefs.isSmsEnabled());

        NotificationPreferenceRequest updateRequest = NotificationPreferenceRequest.builder()
                .inAppEnabled(true)
                .pushEnabled(false)
                .emailEnabled(true)
                .smsEnabled(false)
                .build();

        NotificationPreferenceResponse updated = notificationService.updateNotificationPreferences(100L, updateRequest);
        assertNotNull(updated);
        assertTrue(updated.isInAppEnabled());
        assertFalse(updated.isPushEnabled());
        assertTrue(updated.isEmailEnabled());
    }
}
