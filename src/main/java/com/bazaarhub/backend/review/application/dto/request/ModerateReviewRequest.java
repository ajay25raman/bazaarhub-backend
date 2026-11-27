package com.bazaarhub.backend.review.application.dto.request;

import com.bazaarhub.backend.review.domain.model.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin review moderation request")
public class ModerateReviewRequest {

    @NotNull(message = "Moderation status is required")
    @Schema(description = "Target moderation status (approved or rejected)", example = "approved")
    private ReviewStatus status;

    @Schema(description = "Optional internal moderation reason or note", example = "Review verified and complies with community guidelines")
    private String reason;
}
