package com.bazaarhub.backend.customer.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer profile image URL update request")
public class UpdateProfileImageRequest {

    @NotBlank(message = "Profile image URL is required")
    @Size(max = 500, message = "Profile image URL must not exceed 500 characters")
    @URL(message = "Profile image URL must be a valid URL")
    @Schema(description = "Profile image URL", example = "https://images.bazaarhub.com/profiles/alice.jpg")
    private String profileImageUrl;
}
