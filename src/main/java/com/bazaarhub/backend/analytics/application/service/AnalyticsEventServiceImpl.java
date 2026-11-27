package com.bazaarhub.backend.analytics.application.service;

import com.bazaarhub.backend.analytics.application.dto.request.LogAnalyticsEventRequest;
import com.bazaarhub.backend.analytics.infrastructure.persistence.entity.AnalyticsEventEntity;
import com.bazaarhub.backend.analytics.infrastructure.persistence.repository.SpringDataAnalyticsEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
public class AnalyticsEventServiceImpl implements AnalyticsEventService {

    private final SpringDataAnalyticsEventRepository repository;

    public AnalyticsEventServiceImpl(SpringDataAnalyticsEventRepository repository) {
        this.repository = repository;
    }

    @Async
    @Override
    @Transactional
    public void logEvent(LogAnalyticsEventRequest request, Long userId) {
        try {
            AnalyticsEventEntity entity = AnalyticsEventEntity.builder()
                    .userId(userId)
                    .sessionId(request.getSessionId())
                    .eventName(request.getEventName())
                    .entityType(request.getEntityType())
                    .entityId(request.getEntityId())
                    .eventProperties(request.getEventProperties())
                    .platform(request.getPlatform())
                    .createdAt(Instant.now())
                    .build();

            repository.save(entity);
            log.debug("Logged analytics event: {} for user: {}", request.getEventName(), userId);
        } catch (Exception e) {
            log.error("Failed to asynchronously log analytics event: {}", request.getEventName(), e);
        }
    }
}
