package com.bazaarhub.backend.delivery.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response returned after uploading delivery proof or KYC asset")
public class ProofUploadResponse {

    @Schema(description = "Secure asset URL on Cloudinary", example = "https://res.cloudinary.com/demo/image/upload/bazaarhub/delivery/proofs/delivery_101.jpg")
    private String url;

    @Schema(description = "Destination folder", example = "bazaarhub/delivery/proofs")
    private String folder;

    @Schema(description = "Upload timestamp")
    private Instant uploadedAt;
}
