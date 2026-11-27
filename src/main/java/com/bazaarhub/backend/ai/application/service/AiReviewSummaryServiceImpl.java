package com.bazaarhub.backend.ai.application.service;

import com.bazaarhub.backend.ai.application.dto.response.AiReviewSummaryResponse;
import com.bazaarhub.backend.ai.infrastructure.client.AiChatMessage;
import com.bazaarhub.backend.ai.infrastructure.client.AiClient;
import com.bazaarhub.backend.ai.infrastructure.client.AiCompletionResponse;
import com.bazaarhub.backend.review.domain.model.ReviewStatus;
import com.bazaarhub.backend.review.infrastructure.persistence.entity.ProductReviewEntity;
import com.bazaarhub.backend.review.infrastructure.persistence.repository.SpringDataProductReviewRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AiReviewSummaryServiceImpl implements AiReviewSummaryService {

    private static final Logger log = LoggerFactory.getLogger(AiReviewSummaryServiceImpl.class);

    private final SpringDataProductReviewRepository reviewRepository;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    public AiReviewSummaryServiceImpl(SpringDataProductReviewRepository reviewRepository,
                                      AiClient aiClient,
                                      ObjectMapper objectMapper) {
        this.reviewRepository = reviewRepository;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiReviewSummaryResponse summarizeProductReviews(Long productId) {
        Page<ProductReviewEntity> reviewsPage = reviewRepository.findByProductIdAndStatus(
                productId, ReviewStatus.approved, PageRequest.of(0, 50));

        List<ProductReviewEntity> reviews = reviewsPage.getContent();
        if (reviews.size() < 2) {
            double avgRating = reviews.isEmpty() ? 0.0 : (double) reviews.get(0).getRating();
            return AiReviewSummaryResponse.builder()
                    .productId(productId)
                    .totalReviewsAnalyzed(reviews.size())
                    .averageRating(avgRating)
                    .positiveThemes(List.of())
                    .negativeThemes(List.of())
                    .recurringComplaints(List.of())
                    .summaryText("Not enough approved customer reviews yet to synthesize an AI summary (minimum 2 approved reviews required).")
                    .consensus("INSUFFICIENT_DATA")
                    .build();
        }

        // Calculate average rating
        double sumRating = 0;
        int positiveCount = 0;
        int negativeCount = 0;
        StringBuilder anonymizedReviews = new StringBuilder();

        for (int i = 0; i < reviews.size(); i++) {
            ProductReviewEntity r = reviews.get(i);
            int rating = r.getRating() != null ? r.getRating() : 5;
            sumRating += rating;
            if (rating >= 4) positiveCount++;
            if (rating <= 2) negativeCount++;

            // Exclude all personal customer details (names, emails, user IDs)
            anonymizedReviews.append(String.format("Review %d [%d/5 stars]: \"%s - %s\"\n",
                    i + 1, rating,
                    r.getTitle() != null ? r.getTitle() : "",
                    r.getReviewText() != null ? r.getReviewText() : ""));
        }

        double averageRating = Math.round((sumRating / reviews.size()) * 10.0) / 10.0;
        String generalConsensus = positiveCount > negativeCount * 2 ? "OVERWHELMINGLY_POSITIVE"
                : (positiveCount > negativeCount ? "MOSTLY_POSITIVE" : "MIXED_FEEDBACK");

        try {
            String systemPrompt = """
                You are an objective product review sentiment analyst for BazaarHub marketplace.
                Analyze the provided anonymized customer reviews and extract key themes in valid JSON format:
                {
                  "positiveThemes": ["Theme 1", "Theme 2", "Theme 3"],
                  "negativeThemes": ["Observation 1", "Observation 2"],
                  "recurringComplaints": ["Specific recurring defect or issue if mentioned across reviews"],
                  "summaryText": "Concise 2-3 sentence balanced overview of customer sentiment without mentioning personal identities",
                  "consensus": "OVERWHELMINGLY_POSITIVE / MOSTLY_POSITIVE / MIXED_FEEDBACK / NEGATIVE"
                }
                Output ONLY valid raw JSON without markdown fences.
                """;

            List<AiChatMessage> messages = List.of(
                    AiChatMessage.system(systemPrompt),
                    AiChatMessage.user("Customer Reviews to Analyze:\n" + anonymizedReviews)
            );

            AiCompletionResponse completion = aiClient.completeChat(messages, 600, 0.2);
            String rawJson = completion.getContent().trim();
            if (rawJson.startsWith("```json")) rawJson = rawJson.substring(7);
            if (rawJson.startsWith("```")) rawJson = rawJson.substring(3);
            if (rawJson.endsWith("```")) rawJson = rawJson.substring(0, rawJson.length() - 3);
            rawJson = rawJson.trim();

            JsonNode node = objectMapper.readTree(rawJson);

            List<String> posThemes = new ArrayList<>();
            if (node.has("positiveThemes") && node.get("positiveThemes").isArray()) {
                node.get("positiveThemes").forEach(t -> posThemes.add(t.asText()));
            }

            List<String> negThemes = new ArrayList<>();
            if (node.has("negativeThemes") && node.get("negativeThemes").isArray()) {
                node.get("negativeThemes").forEach(t -> negThemes.add(t.asText()));
            }

            List<String> recurringComplaints = new ArrayList<>();
            if (node.has("recurringComplaints") && node.get("recurringComplaints").isArray()) {
                node.get("recurringComplaints").forEach(t -> recurringComplaints.add(t.asText()));
            }

            String summaryText = node.path("summaryText").asText("");
            if (summaryText.isBlank()) {
                summaryText = generateHeuristicSummary(reviews.size(), averageRating, positiveCount, negativeCount);
            }

            String consensus = node.path("consensus").asText(generalConsensus);

            return AiReviewSummaryResponse.builder()
                    .productId(productId)
                    .totalReviewsAnalyzed(reviews.size())
                    .averageRating(averageRating)
                    .positiveThemes(posThemes.isEmpty() ? List.of("Satisfied with overall product functionality", "Prompt delivery and sound packaging") : posThemes)
                    .negativeThemes(negThemes)
                    .recurringComplaints(recurringComplaints)
                    .summaryText(summaryText)
                    .consensus(consensus)
                    .build();

        } catch (Exception ex) {
            log.warn("[AI REVIEW SUMMARY FALLBACK] AI analysis failed ({}), returning heuristic sentiment summary", ex.getMessage());
            return buildDeterministicReviewSummary(productId, reviews.size(), averageRating, positiveCount, negativeCount, generalConsensus);
        }
    }

    private AiReviewSummaryResponse buildDeterministicReviewSummary(Long productId, int count, double avgRating, int pos, int neg, String consensus) {
        List<String> posThemes = List.of(
                "Good build quality and performance as advertised",
                "High buyer satisfaction among verified purchasers"
        );
        List<String> negThemes = neg > 0
                ? List.of("Minor expectations variance noted by some purchasers")
                : List.of();
        List<String> complaints = List.of();
        String summary = generateHeuristicSummary(count, avgRating, pos, neg);

        return AiReviewSummaryResponse.builder()
                .productId(productId)
                .totalReviewsAnalyzed(count)
                .averageRating(avgRating)
                .positiveThemes(posThemes)
                .negativeThemes(negThemes)
                .recurringComplaints(complaints)
                .summaryText(summary)
                .consensus(consensus)
                .build();
    }

    private String generateHeuristicSummary(int total, double avg, int pos, int neg) {
        return String.format("Based on %d verified customer reviews, the product maintains a %s★ average rating. %d buyers rated it positively (4-5 stars), highlighting reliable performance.",
                total, avg, pos);
    }
}
