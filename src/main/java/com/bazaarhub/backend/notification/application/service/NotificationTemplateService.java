package com.bazaarhub.backend.notification.application.service;

import com.bazaarhub.backend.notification.application.dto.request.CreateNotificationTemplateRequest;
import com.bazaarhub.backend.notification.application.dto.request.UpdateNotificationTemplateRequest;
import com.bazaarhub.backend.notification.application.dto.response.NotificationTemplateResponse;
import com.bazaarhub.backend.notification.domain.model.NotificationChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface NotificationTemplateService {

    NotificationTemplateResponse createTemplate(CreateNotificationTemplateRequest request, Long adminId);

    NotificationTemplateResponse updateTemplate(Long templateId, UpdateNotificationTemplateRequest request, Long adminId);

    NotificationTemplateResponse getTemplateById(Long templateId);

    NotificationTemplateResponse getTemplateByKeyAndChannel(String templateKey, NotificationChannel channel);

    Page<NotificationTemplateResponse> getAllTemplates(Pageable pageable);

    String renderTemplate(String templateKey, NotificationChannel channel, Map<String, Object> variables, String fallbackText);

    String interpolate(String templateBody, Map<String, Object> variables);
}
