package com.bazaarhub.backend.analytics.application.dto.request;

import com.bazaarhub.backend.analytics.domain.model.AnalyticsPlatform;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogAnalyticsEventRequest {

    private String sessionId;

    @NotBlank(message = "Event name is mandatory")
    private String eventName;

    private String entityType;

    private Long entityId;

    private String eventProperties;

    private AnalyticsPlatform platform;
}
