package com.bazaarhub.backend.ai.infrastructure.persistence.repository;

import com.bazaarhub.backend.ai.infrastructure.persistence.entity.AiConversationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataAiConversationRepository extends JpaRepository<AiConversationEntity, Long> {

    Page<AiConversationEntity> findByUserIdOrderByLastMessageAtDesc(Long userId, Pageable pageable);

    Optional<AiConversationEntity> findByIdAndUserId(Long id, Long userId);
}
