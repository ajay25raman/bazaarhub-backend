package com.bazaarhub.backend.support.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Strongly-typed request payload for support ticket attachment uploads.
 * Replaces unstructured Map<String, String> payloads to prevent NPEs and enforce validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Support ticket attachment upload payload")
public class AttachmentUploadRequest {

    @NotBlank(message = "Attachment image content is required")
    @Size(max = 1_000_000, message = "Attachment image content exceeds maximum permitted size")
    @Schema(description = "Base64 data URI or image URL to upload", example = "https://example.com/photo.png")
    private String image;
}
