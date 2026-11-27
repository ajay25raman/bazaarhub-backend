package com.bazaarhub.backend.support.application.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload to add a reply or note to a support ticket")
public class AddTicketMessageRequest {

    @NotBlank(message = "Message content is required")
    @Schema(description = "Reply text or internal note description", example = "We have contacted the courier and requested an urgent re-delivery.")
    private String message;

    @Size(max = 500, message = "Attachment URL must not exceed 500 characters")
    @Schema(description = "Optional Cloudinary uploaded image or document URL", example = "https://res.cloudinary.com/demo/image/upload/v1/support/proof.jpg")
    private String attachmentUrl;

    @JsonProperty("isInternal")
    @JsonAlias("internal")
    @Builder.Default
    @Schema(description = "Whether this message is an internal agent note (only support staff/admin can set this to true)", example = "false")
    private Boolean isInternal = false;

    public boolean isInternal() {
        return Boolean.TRUE.equals(this.isInternal);
    }

    public void setInternal(boolean internal) {
        this.isInternal = internal;
    }
}
