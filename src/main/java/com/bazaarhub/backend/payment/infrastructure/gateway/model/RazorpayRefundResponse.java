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
public class RazorpayRefundResponse {
    private String id;
    private String paymentId;
    private Long amount; // in paise
    private BigDecimal amountDecimal;
    private String currency;
    private String status;
    private String receipt;
}
