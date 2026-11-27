package com.bazaarhub.backend.payment.infrastructure.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrderResponse {
    private String id;
    private Long amount; // in smallest currency unit (paise)
    private BigDecimal amountDecimal; // in standard currency (INR)
    private String currency;
    private String receipt;
    private String status;
}
