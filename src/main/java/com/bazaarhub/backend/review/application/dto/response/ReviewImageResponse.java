package com.bazaarhub.backend.review.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Review image asset details")
public class ReviewImageResponse {

    @Schema(description = "Image record ID", example = "42")
    private Long id;

    @Schema(description = "Public CDN image URL", example = "https://res.cloudinary.com/demo/image/upload/bazaarhub/reviews/sample.jpg")
    private String imageUrl;

    @Schema(description = "Display order sequence", example = "0")
    private Integer displayOrder;
}
