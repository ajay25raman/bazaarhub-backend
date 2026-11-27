package com.bazaarhub.backend.ai.application.service;

import com.bazaarhub.backend.ai.application.dto.response.AiProductRecommendationResponse;

import java.util.List;

public interface AiProductRecommendationService {

    List<AiProductRecommendationResponse> getRecommendations(Long categoryId, Long authenticatedUserId, int limit);
}
