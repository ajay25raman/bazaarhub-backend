package com.bazaarhub.backend.analytics.infrastructure.persistence.repository;

import com.bazaarhub.backend.analytics.infrastructure.persistence.entity.AnalyticsEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SpringDataAnalyticsEventRepository extends JpaRepository<AnalyticsEventEntity, Long> {

    long countByCreatedAtBetween(Instant start, Instant end);

    long countByEventNameAndCreatedAtBetween(String eventName, Instant start, Instant end);

    @Query("SELECT COUNT(DISTINCT e.userId) FROM AnalyticsEventEntity e WHERE e.userId IS NOT NULL AND e.createdAt BETWEEN :start AND :end")
    long countDistinctActiveUsersBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT e.eventName, COUNT(e) FROM AnalyticsEventEntity e WHERE e.createdAt BETWEEN :start AND :end GROUP BY e.eventName ORDER BY COUNT(e) DESC")
    List<Object[]> aggregateEventsByName(@Param("start") Instant start, @Param("end") Instant end);
}
