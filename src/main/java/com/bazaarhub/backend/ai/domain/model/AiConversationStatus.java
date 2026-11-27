package com.bazaarhub.backend.ai.domain.model;

/**
 * Status of an AI conversation session matching MariaDB enum('ACTIVE','ARCHIVED','CLOSED').
 */
public enum AiConversationStatus {
    ACTIVE,
    ARCHIVED,
    CLOSED
}
