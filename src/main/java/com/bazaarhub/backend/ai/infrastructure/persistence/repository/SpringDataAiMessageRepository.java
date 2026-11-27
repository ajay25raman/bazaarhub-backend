package com.bazaarhub.backend.ai.infrastructure.persistence.repository;

import com.bazaarhub.backend.ai.infrastructure.persistence.entity.AiMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataAiMessageRepository extends JpaRepository<AiMessageEntity, Long> {

    List<AiMessageEntity> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    Page<AiMessageEntity> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);
}
