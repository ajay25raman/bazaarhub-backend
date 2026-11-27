package com.bazaarhub.backend.notification;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.notification.application.dto.request.CreateNotificationTemplateRequest;
import com.bazaarhub.backend.notification.application.dto.request.UpdateNotificationTemplateRequest;
import com.bazaarhub.backend.notification.application.dto.response.NotificationTemplateResponse;
import com.bazaarhub.backend.notification.application.mapper.NotificationTemplateMapper;
import com.bazaarhub.backend.notification.application.service.NotificationTemplateServiceImpl;
import com.bazaarhub.backend.notification.domain.exception.InvalidNotificationException;
import com.bazaarhub.backend.notification.domain.exception.NotificationTemplateNotFoundException;
import com.bazaarhub.backend.notification.domain.model.NotificationChannel;
import com.bazaarhub.backend.notification.domain.model.TemplateStatus;
import com.bazaarhub.backend.notification.infrastructure.persistence.entity.NotificationTemplateEntity;
import com.bazaarhub.backend.notification.infrastructure.persistence.repository.SpringDataNotificationTemplateRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationTemplateServiceUnitTest {

    @Mock
    private SpringDataNotificationTemplateRepository templateRepository;

    @Mock
    private SpringDataUserRepository userRepository;

    @Spy
    private NotificationTemplateMapper templateMapper = new NotificationTemplateMapper();

    @InjectMocks
    private NotificationTemplateServiceImpl templateService;

    private UserEntity admin;

    @BeforeEach
    void setUp() {
        admin = UserEntity.builder()
                .id(1L)
                .email("admin@bazaarhub.com")
                .fullName("Admin Super")
                .build();
    }

    @Test
    @DisplayName("Create template: Success with uppercase key and active status")
    void testCreateTemplate_Success() {
        CreateNotificationTemplateRequest request = CreateNotificationTemplateRequest.builder()
                .templateKey("order_created_email")
                .channel(NotificationChannel.EMAIL)
                .subject("Order Placed: {{orderNumber}}")
                .bodyTemplate("Hello {{customerName}}, your order {{orderNumber}} is confirmed.")
                .status(TemplateStatus.ACTIVE)
                .build();

        when(templateRepository.existsByTemplateKeyAndChannel("order_created_email", NotificationChannel.EMAIL))
                .thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        NotificationTemplateEntity savedEntity = NotificationTemplateEntity.builder()
                .id(10L)
                .templateKey("ORDER_CREATED_EMAIL")
                .channel(NotificationChannel.EMAIL)
                .subject("Order Placed: {{orderNumber}}")
                .bodyTemplate("Hello {{customerName}}, your order {{orderNumber}} is confirmed.")
                .status(TemplateStatus.ACTIVE)
                .createdBy(admin)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(templateRepository.save(any(NotificationTemplateEntity.class))).thenReturn(savedEntity);

        NotificationTemplateResponse response = templateService.createTemplate(request, 1L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("ORDER_CREATED_EMAIL", response.getTemplateKey());
        assertEquals(NotificationChannel.EMAIL, response.getChannel());
        assertEquals(TemplateStatus.ACTIVE, response.getStatus());

        verify(templateRepository).save(any(NotificationTemplateEntity.class));
    }

    @Test
    @DisplayName("Create template: Duplicate template key and channel throws InvalidNotificationException")
    void testCreateTemplate_Duplicate_ThrowsException() {
        CreateNotificationTemplateRequest request = CreateNotificationTemplateRequest.builder()
                .templateKey("ORDER_CREATED")
                .channel(NotificationChannel.IN_APP)
                .bodyTemplate("You have an order.")
                .build();

        when(templateRepository.existsByTemplateKeyAndChannel("ORDER_CREATED", NotificationChannel.IN_APP))
                .thenReturn(true);

        assertThrows(InvalidNotificationException.class, () ->
                templateService.createTemplate(request, 1L));

        verify(templateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update template: Updates subject, body, and status successfully")
    void testUpdateTemplate_Success() {
        NotificationTemplateEntity existing = NotificationTemplateEntity.builder()
                .id(10L)
                .templateKey("ORDER_CREATED")
                .channel(NotificationChannel.PUSH)
                .subject("Old Subject")
                .bodyTemplate("Old Body")
                .status(TemplateStatus.ACTIVE)
                .build();

        UpdateNotificationTemplateRequest updateRequest = UpdateNotificationTemplateRequest.builder()
                .subject("New Subject")
                .bodyTemplate("New Body with {{code}}")
                .status(TemplateStatus.INACTIVE)
                .build();

        when(templateRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(templateRepository.save(any(NotificationTemplateEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationTemplateResponse response = templateService.updateTemplate(10L, updateRequest, 1L);

        assertNotNull(response);
        assertEquals("New Subject", response.getSubject());
        assertEquals("New Body with {{code}}", response.getBodyTemplate());
        assertEquals(TemplateStatus.INACTIVE, response.getStatus());
    }

    @Test
    @DisplayName("Get template by ID: Throws NotificationTemplateNotFoundException when missing")
    void testGetTemplateById_NotFound() {
        when(templateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotificationTemplateNotFoundException.class, () ->
                templateService.getTemplateById(999L));
    }

    @Test
    @DisplayName("Interpolate: Replaces {{key}} and ${key} placeholders accurately")
    void testInterpolate_ReplacesPlaceholders() {
        String template = "Hello {{customerName}}, your OTP is ${otp} and amount is {{amount}} INR.";
        Map<String, Object> variables = Map.of(
                "customerName", "Alice",
                "otp", 456789,
                "amount", "1299.50"
        );

        String result = templateService.interpolate(template, variables);

        assertEquals("Hello Alice, your OTP is 456789 and amount is 1299.50 INR.", result);
    }

    @Test
    @DisplayName("Render template: Falls back to fallbackText when template not found in database")
    void testRenderTemplate_Fallback() {
        when(templateRepository.findByTemplateKeyAndChannel("NON_EXISTENT", NotificationChannel.EMAIL))
                .thenReturn(Optional.empty());

        String rendered = templateService.renderTemplate("NON_EXISTENT", NotificationChannel.EMAIL,
                Map.of("name", "Bob"), "Fallback message for {{name}}");

        assertEquals("Fallback message for Bob", rendered);
    }
}
