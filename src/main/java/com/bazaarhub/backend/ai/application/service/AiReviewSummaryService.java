package com.bazaarhub.backend.ai.application.service;

import com.bazaarhub.backend.ai.application.dto.response.AiReviewSummaryResponse;

public interface AiReviewSummaryService {

    AiReviewSummaryResponse summarizeProductReviews(Long productId);
}
