package com.bazaarhub.backend.notification.application.service;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.notification.application.dto.request.CreateNotificationTemplateRequest;
import com.bazaarhub.backend.notification.application.dto.request.UpdateNotificationTemplateRequest;
import com.bazaarhub.backend.notification.application.dto.response.NotificationTemplateResponse;
import com.bazaarhub.backend.notification.application.mapper.NotificationTemplateMapper;
import com.bazaarhub.backend.notification.domain.exception.InvalidNotificationException;
import com.bazaarhub.backend.notification.domain.exception.NotificationTemplateNotFoundException;
import com.bazaarhub.backend.notification.domain.model.NotificationChannel;
import com.bazaarhub.backend.notification.domain.model.TemplateStatus;
import com.bazaarhub.backend.notification.infrastructure.persistence.entity.NotificationTemplateEntity;
import com.bazaarhub.backend.notification.infrastructure.persistence.repository.SpringDataNotificationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    private static final Logger log = LoggerFactory.getLogger(NotificationTemplateServiceImpl.class);

    private final SpringDataNotificationTemplateRepository templateRepository;
    private final SpringDataUserRepository userRepository;
    private final NotificationTemplateMapper templateMapper;

    public NotificationTemplateServiceImpl(SpringDataNotificationTemplateRepository templateRepository,
                                           SpringDataUserRepository userRepository,
                                           NotificationTemplateMapper templateMapper) {
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
        this.templateMapper = templateMapper;
    }

    @Override
    public NotificationTemplateResponse createTemplate(CreateNotificationTemplateRequest request, Long adminId) {
        log.info("[NOTIFICATION TEMPLATE] Creating template '{}' for channel '{}'", request.getTemplateKey(), request.getChannel());

        if (templateRepository.existsByTemplateKeyAndChannel(request.getTemplateKey(), request.getChannel())) {
            throw new InvalidNotificationException(String.format("Template with key '%s' and channel '%s' already exists",
                    request.getTemplateKey(), request.getChannel()));
        }

        UserEntity admin = adminId != null ? userRepository.findById(adminId).orElse(null) : null;

        NotificationTemplateEntity entity = NotificationTemplateEntity.builder()
                .templateKey(request.getTemplateKey().trim().toUpperCase())
                .channel(request.getChannel())
                .subject(request.getSubject() != null ? request.getSubject().trim() : null)
                .bodyTemplate(request.getBodyTemplate().trim())
                .status(request.getStatus() != null ? request.getStatus() : TemplateStatus.ACTIVE)
                .createdBy(admin)
                .updatedBy(admin)
                .build();

        NotificationTemplateEntity saved = templateRepository.save(entity);
        log.info("[NOTIFICATION TEMPLATE CREATED] Successfully created template ID: {}", saved.getId());
        return templateMapper.toResponse(saved);
    }

    @Override
    public NotificationTemplateResponse updateTemplate(Long templateId, UpdateNotificationTemplateRequest request, Long adminId) {
        log.info("[NOTIFICATION TEMPLATE UPDATE] Updating template ID: {}", templateId);

        NotificationTemplateEntity entity = templateRepository.findById(templateId)
                .orElseThrow(() -> new NotificationTemplateNotFoundException(templateId));

        UserEntity admin = adminId != null ? userRepository.findById(adminId).orElse(null) : null;

        if (request.getSubject() != null) {
            entity.setSubject(request.getSubject().trim());
        }
        if (request.getBodyTemplate() != null && !request.getBodyTemplate().isBlank()) {
            entity.setBodyTemplate(request.getBodyTemplate().trim());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        entity.setUpdatedBy(admin);

        NotificationTemplateEntity updated = templateRepository.save(entity);
        return templateMapper.toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationTemplateResponse getTemplateById(Long templateId) {
        return templateRepository.findById(templateId)
                .map(templateMapper::toResponse)
                .orElseThrow(() -> new NotificationTemplateNotFoundException(templateId));
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationTemplateResponse getTemplateByKeyAndChannel(String templateKey, NotificationChannel channel) {
        return templateRepository.findByTemplateKeyAndChannel(templateKey, channel)
                .map(templateMapper::toResponse)
                .orElseThrow(() -> new NotificationTemplateNotFoundException(templateKey, channel.name()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationTemplateResponse> getAllTemplates(Pageable pageable) {
        return templateRepository.findAll(pageable).map(templateMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public String renderTemplate(String templateKey, NotificationChannel channel, Map<String, Object> variables, String fallbackText) {
        if (templateKey == null || templateKey.isBlank()) {
            return interpolate(fallbackText, variables);
        }

        return templateRepository.findByTemplateKeyAndChannel(templateKey, channel)
                .filter(t -> t.getStatus() == TemplateStatus.ACTIVE)
                .map(t -> interpolate(t.getBodyTemplate(), variables))
                .orElseGet(() -> interpolate(fallbackText, variables));
    }

    @Override
    public String interpolate(String templateBody, Map<String, Object> variables) {
        if (templateBody == null) {
            return "";
        }
        if (variables == null || variables.isEmpty()) {
            return templateBody;
        }

        String result = templateBody;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String replacement = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
            result = result.replace("{{" + entry.getKey() + "}}", replacement)
                           .replace("${" + entry.getKey() + "}", replacement);
        }
        return result;
    }
}
