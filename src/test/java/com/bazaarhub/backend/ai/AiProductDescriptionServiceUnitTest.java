package com.bazaarhub.backend.ai;

import com.bazaarhub.backend.ai.application.dto.request.AiGenerateDescriptionRequest;
import com.bazaarhub.backend.ai.application.dto.response.AiProductDescriptionResponse;
import com.bazaarhub.backend.ai.application.service.AiProductDescriptionServiceImpl;
import com.bazaarhub.backend.ai.infrastructure.client.AiClient;
import com.bazaarhub.backend.ai.infrastructure.client.AiCompletionResponse;
import com.bazaarhub.backend.ai.infrastructure.security.AiPromptSanitizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiProductDescriptionServiceUnitTest {

    @Mock
    private AiClient aiClient;

    @Spy
    private AiPromptSanitizer promptSanitizer = new AiPromptSanitizer();

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AiProductDescriptionServiceImpl service;

    @Test
    @DisplayName("Should generate draft description with suggested titles and structured copy")
    void shouldGenerateDraftDescription() {
        AiGenerateDescriptionRequest request = AiGenerateDescriptionRequest.builder()
                .productName("Organic Raw Honey 500g")
                .categoryName("Groceries")
                .brandName("BazaarFarm")
                .keyFeatures(List.of("100% pure", "Unpasteurized", "Wildflower nectar"))
                .targetAudience("Health conscious consumers")
                .build();

        String rawJson = """
                {
                  "suggestedTitles": ["Pure Wildflower Honey 500g", "Raw Unfiltered Honey - 100% Organic"],
                  "shortDescription": "Authentic raw wildflower honey, harvested ethically.",
                  "longDescription": "Experience nature's purest sweetener with our 100% raw unpasteurized honey.",
                  "keyFeatures": ["100% Pure Raw Honey", "Unfiltered & Unheated", "Rich in natural enzymes"],
                  "seoTitle": "Raw Wildflower Honey 500g | BazaarHub",
                  "seoDescription": "Buy 100% pure raw wildflower honey on BazaarHub."
                }
                """;

        when(aiClient.completeChat(anyList(), anyInt(), anyDouble()))
                .thenReturn(AiCompletionResponse.builder()
                        .content(rawJson)
                        .model("openai/gpt-oss-20b")
                        .promptTokens(200)
                        .completionTokens(150)
                        .totalTokens(350)
                        .latencyMs(250)
                        .build());

        AiProductDescriptionResponse response = service.generateContent(request, 10L);

        assertNotNull(response);
        assertEquals("DRAFT", response.getStatus(), "AI descriptions must always remain in DRAFT status");
        assertFalse(response.getSuggestedTitles().isEmpty());
        assertEquals("Pure Wildflower Honey 500g", response.getSuggestedTitles().get(0));
        assertNotNull(response.getLongDescription());
        assertEquals(3, response.getKeyFeatures().size());
    }

    @Test
    @DisplayName("Should fall back to deterministic draft template when AI call fails")
    void shouldFallBackWhenAiCallFails() {
        AiGenerateDescriptionRequest request = AiGenerateDescriptionRequest.builder()
                .productName("Wireless Mouse")
                .categoryName("Electronics")
                .build();

        when(aiClient.completeChat(anyList(), anyInt(), anyDouble()))
                .thenThrow(new RuntimeException("AI Gateway timeout"));

        AiProductDescriptionResponse response = service.generateContent(request, 10L);

        assertNotNull(response);
        assertEquals("DRAFT", response.getStatus());
        assertFalse(response.getSuggestedTitles().isEmpty());
        assertTrue(response.getSuggestedTitles().get(0).contains("Wireless Mouse"));
    }
}
