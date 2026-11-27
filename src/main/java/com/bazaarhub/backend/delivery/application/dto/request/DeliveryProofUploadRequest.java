package com.bazaarhub.backend.delivery.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Strongly-typed request payload for delivery proof and obstacle image uploads.
 * Replaces unstructured Map<String, String> payloads to prevent unvalidated Cloudinary proxying.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Delivery proof image upload payload")
public class DeliveryProofUploadRequest {

    @Positive(message = "Shipment ID must be positive")
    @Schema(description = "Associated Shipment ID", example = "101")
    private Long shipmentId;

    @NotBlank(message = "Proof image content is required")
    @Size(max = 1_000_000, message = "Proof image content exceeds maximum permitted size")
    @Schema(description = "Base64 data URI or image URL of proof", example = "data:image/jpeg;base64,...")
    private String image;

    @Size(max = 200, message = "Folder name must not exceed 200 characters")
    @Schema(description = "Optional destination Cloudinary folder", example = "bazaarhub/delivery/proofs")
    private String folder;
}
