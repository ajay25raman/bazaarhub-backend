package com.bazaarhub.backend.review.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer product review update request")
public class UpdateReviewRequest {

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    @Schema(description = "Updated rating score from 1 to 5 stars", example = "4")
    private Integer rating;

    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Schema(description = "Updated review headline or title", example = "Updated: Still very good after 1 month of use")
    private String title;

    @NotBlank(message = "Review text is required")
    @Size(min = 5, max = 5000, message = "Review text must be between 5 and 5000 characters")
    @Schema(description = "Updated detailed review body", example = "After a month of usage, battery life holds up well. Highly recommended.")
    private String reviewText;

    @Builder.Default
    @Schema(description = "Updated list of review image URLs or base64 uploads (max 5)")
    private List<String> imageUrls = new ArrayList<>();
}
