package com.bazaarhub.backend.ai.application.service;

import com.bazaarhub.backend.ai.application.dto.request.AiGenerateDescriptionRequest;
import com.bazaarhub.backend.ai.application.dto.response.AiProductDescriptionResponse;

public interface AiProductDescriptionService {

    AiProductDescriptionResponse generateContent(AiGenerateDescriptionRequest request, Long requestingUserId);
}
