package com.bazaarhub.backend.ai.application.service;

import com.bazaarhub.backend.ai.application.dto.request.AiChatRequest;
import com.bazaarhub.backend.ai.application.dto.response.AiChatResponse;

public interface AiSupportAssistantService {

    AiChatResponse chat(AiChatRequest request, Long authenticatedUserId);
}
