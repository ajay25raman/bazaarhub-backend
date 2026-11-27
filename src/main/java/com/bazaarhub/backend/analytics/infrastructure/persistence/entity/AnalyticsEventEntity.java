package com.bazaarhub.backend.analytics.infrastructure.persistence.entity;

import com.bazaarhub.backend.analytics.domain.model.AnalyticsPlatform;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "analytics_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_id", length = 150)
    private String sessionId;

    @Column(name = "event_name", nullable = false, length = 150)
    private String eventName;

    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "event_properties", columnDefinition = "LONGTEXT")
    private String eventProperties;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", length = 20)
    private AnalyticsPlatform platform;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
