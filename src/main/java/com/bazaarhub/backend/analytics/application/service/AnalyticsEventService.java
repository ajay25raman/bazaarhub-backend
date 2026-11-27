package com.bazaarhub.backend.analytics.application.service;

import com.bazaarhub.backend.analytics.application.dto.request.LogAnalyticsEventRequest;

public interface AnalyticsEventService {
    void logEvent(LogAnalyticsEventRequest request, Long userId);
}
