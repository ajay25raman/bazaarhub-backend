package com.bazaarhub.backend.ai.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI-generated synthesis of verified, approved customer product reviews")
public class AiReviewSummaryResponse {

    @Schema(description = "Product ID", example = "42")
    private Long productId;

    @Schema(description = "Number of approved reviews analyzed", example = "28")
    private int totalReviewsAnalyzed;

    @Schema(description = "Aggregated average star rating", example = "4.6")
    private Double averageRating;

    @Schema(description = "Key positive attributes and praised aspects", example = "[\"Excellent battery life exceeding expectations\", \"Comfortable memory foam ear cushions for long sessions\", \"Fast Bluetooth pairing\"]")
    private List<String> positiveThemes;

    @Schema(description = "Negative observations and areas for improvement", example = "[\"Bass is slightly heavy for classical genres\", \"Carry pouch feels thin\"]")
    private List<String> negativeThemes;

    @Schema(description = "Recurring complaints or defect flags (if any)", example = "[\"Microphone volume can be low in windy outdoor environments\"]")
    private List<String> recurringComplaints;

    @Schema(description = "Synthesized overall customer consensus summary text", example = "Customers overwhelmingly appreciate the build quality, sound clarity, and battery life, with minor complaints centered around outdoor microphone clarity.")
    private String summaryText;

    @Schema(description = "Overall sentiment consensus label", example = "OVERWHELMINGLY_POSITIVE")
    private String consensus;
}
