package com.bazaarhub.backend.customer.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer gender update request")
public class UpdateGenderRequest {

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "(?i)^(MALE|FEMALE|OTHER|PREFER_NOT_TO_SAY)$", message = "Gender must be MALE, FEMALE, OTHER, or PREFER_NOT_TO_SAY")
    @Schema(description = "Gender", example = "FEMALE", allowableValues = {"MALE", "FEMALE", "OTHER", "PREFER_NOT_TO_SAY"})
    private String gender;
}
