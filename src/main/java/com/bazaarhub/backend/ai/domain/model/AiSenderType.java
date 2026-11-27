package com.bazaarhub.backend.ai.domain.model;

/**
 * Sender type of an AI conversation message matching MariaDB enum('USER','ASSISTANT','SYSTEM').
 */
public enum AiSenderType {
    USER,
    ASSISTANT,
    SYSTEM
}
