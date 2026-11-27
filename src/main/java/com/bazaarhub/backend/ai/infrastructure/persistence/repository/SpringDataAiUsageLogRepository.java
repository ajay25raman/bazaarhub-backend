package com.bazaarhub.backend.ai.infrastructure.persistence.repository;

import com.bazaarhub.backend.ai.infrastructure.persistence.entity.AiUsageLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Repository
public interface SpringDataAiUsageLogRepository extends JpaRepository<AiUsageLogEntity, Long> {

    Page<AiUsageLogEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT COUNT(u) FROM AiUsageLogEntity u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    long countLogsBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT COALESCE(SUM(u.inputTokens), 0) FROM AiUsageLogEntity u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    long sumInputTokensBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT COALESCE(SUM(u.outputTokens), 0) FROM AiUsageLogEntity u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    long sumOutputTokensBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT COALESCE(SUM(u.estimatedCost), 0) FROM AiUsageLogEntity u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumEstimatedCostBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT u.featureName, COUNT(u), COALESCE(SUM(u.inputTokens + u.outputTokens), 0), COALESCE(SUM(u.estimatedCost), 0) " +
           "FROM AiUsageLogEntity u WHERE u.createdAt BETWEEN :startDate AND :endDate GROUP BY u.featureName")
    List<Object[]> aggregateUsageByFeature(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
}
