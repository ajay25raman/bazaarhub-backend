package com.bazaarhub.backend.payment.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "bazaarhub.payment.cod")
@Getter
@Setter
public class CodProperties {

    private boolean enabled = true;
    private BigDecimal maxAmount = new BigDecimal("50000.00");
}
