package com.bazaarhub.backend.analytics;

import com.bazaarhub.backend.analytics.application.dto.request.LogAnalyticsEventRequest;
import com.bazaarhub.backend.analytics.application.service.AnalyticsEventServiceImpl;
import com.bazaarhub.backend.analytics.domain.model.AnalyticsPlatform;
import com.bazaarhub.backend.analytics.infrastructure.persistence.entity.AnalyticsEventEntity;
import com.bazaarhub.backend.analytics.infrastructure.persistence.repository.SpringDataAnalyticsEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsEventServiceUnitTest {

    @Mock
    private SpringDataAnalyticsEventRepository repository;

    @InjectMocks
    private AnalyticsEventServiceImpl service;

    @Test
    @DisplayName("Should save analytics event entity correctly")
    void shouldSaveAnalyticsEvent() {
        LogAnalyticsEventRequest request = LogAnalyticsEventRequest.builder()
                .sessionId("sess-12345")
                .eventName("PRODUCT_VIEW")
                .entityType("PRODUCT")
                .entityId(101L)
                .eventProperties("{\"source\":\"recommendation_widget\"}")
                .platform(AnalyticsPlatform.WEB)
                .build();

        service.logEvent(request, 42L);

        ArgumentCaptor<AnalyticsEventEntity> captor = ArgumentCaptor.forClass(AnalyticsEventEntity.class);
        verify(repository, times(1)).save(captor.capture());

        AnalyticsEventEntity saved = captor.getValue();
        assertNotNull(saved);
        assertEquals(42L, saved.getUserId());
        assertEquals("sess-12345", saved.getSessionId());
        assertEquals("PRODUCT_VIEW", saved.getEventName());
        assertEquals("PRODUCT", saved.getEntityType());
        assertEquals(101L, saved.getEntityId());
        assertEquals(AnalyticsPlatform.WEB, saved.getPlatform());
        assertNotNull(saved.getCreatedAt());
    }
}
