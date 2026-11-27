package com.bazaarhub.backend.support.infrastructure.persistence.repository;

import com.bazaarhub.backend.support.infrastructure.persistence.entity.SupportMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataSupportMessageRepository extends JpaRepository<SupportMessageEntity, Long> {

    Page<SupportMessageEntity> findByTicketIdAndInternalFalseOrderByCreatedAtAsc(Long ticketId, Pageable pageable);

    Page<SupportMessageEntity> findByTicketIdOrderByCreatedAtAsc(Long ticketId, Pageable pageable);

    long countByTicketId(Long ticketId);

    long countByTicketIdAndInternalFalse(Long ticketId);
}
