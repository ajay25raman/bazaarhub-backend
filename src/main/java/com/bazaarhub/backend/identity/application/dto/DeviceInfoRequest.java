package com.bazaarhub.backend.identity.application.dto;

import com.bazaarhub.backend.identity.domain.model.DeviceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User device details for session association")
public class DeviceInfoRequest {

    @Schema(description = "Unique device identifier", example = "a1b2c3d4-e5f6-7890")
    private String deviceId;

    @Schema(description = "User-friendly device name", example = "Pixel 8 Pro")
    private String deviceName;

    @Schema(description = "Device platform type", example = "ANDROID")
    private DeviceType deviceType;

    @Schema(description = "FCM or APNS push notification token (stored safely, never exposed in responses)", example = "fcm_token_xyz")
    private String pushToken;
}
