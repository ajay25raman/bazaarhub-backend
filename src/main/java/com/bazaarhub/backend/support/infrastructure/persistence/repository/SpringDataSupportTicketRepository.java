package com.bazaarhub.backend.support.infrastructure.persistence.repository;

import com.bazaarhub.backend.support.domain.model.TicketCategory;
import com.bazaarhub.backend.support.domain.model.TicketPriority;
import com.bazaarhub.backend.support.domain.model.TicketStatus;
import com.bazaarhub.backend.support.infrastructure.persistence.entity.SupportTicketEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataSupportTicketRepository extends JpaRepository<SupportTicketEntity, Long> {

    Optional<SupportTicketEntity> findByTicketNumber(String ticketNumber);

    Page<SupportTicketEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<SupportTicketEntity> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, TicketStatus status, Pageable pageable);

    Page<SupportTicketEntity> findByAssignedToIdOrderByCreatedAtDesc(Long assignedToId, Pageable pageable);

    @Query("SELECT t FROM SupportTicketEntity t WHERE " +
           "(:userId IS NULL OR t.user.id = :userId) AND " +
           "(:orderId IS NULL OR (t.order IS NOT NULL AND t.order.id = :orderId)) AND " +
           "(:category IS NULL OR t.category = :category) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:priority IS NULL OR t.priority = :priority) AND " +
           "(:assignedToId IS NULL OR (t.assignedTo IS NOT NULL AND t.assignedTo.id = :assignedToId))")
    Page<SupportTicketEntity> findByFilters(
            @Param("userId") Long userId,
            @Param("orderId") Long orderId,
            @Param("category") TicketCategory category,
            @Param("status") TicketStatus status,
            @Param("priority") TicketPriority priority,
            @Param("assignedToId") Long assignedToId,
            Pageable pageable);

    long countByStatus(TicketStatus status);

    long countByUserIdAndStatus(Long userId, TicketStatus status);

    @Query("SELECT COUNT(t) FROM SupportTicketEntity t WHERE t.createdAt BETWEEN :start AND :end")
    long countTicketsBetween(@Param("start") java.time.Instant start, @Param("end") java.time.Instant end);

    @Query("SELECT COUNT(t) FROM SupportTicketEntity t WHERE t.status = :status AND t.createdAt BETWEEN :start AND :end")
    long countTicketsByStatusBetween(@Param("status") TicketStatus status, @Param("start") java.time.Instant start, @Param("end") java.time.Instant end);

    @Query("SELECT t.category, COUNT(t) FROM SupportTicketEntity t WHERE t.createdAt BETWEEN :start AND :end GROUP BY t.category")
    List<Object[]> aggregateCategoryCounts(@Param("start") java.time.Instant start, @Param("end") java.time.Instant end);

    @Query("SELECT t.status, COUNT(t) FROM SupportTicketEntity t WHERE t.createdAt BETWEEN :start AND :end GROUP BY t.status")
    List<Object[]> aggregateTicketStatusCounts(@Param("start") java.time.Instant start, @Param("end") java.time.Instant end);
}
