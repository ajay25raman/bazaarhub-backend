package com.bazaarhub.backend.payment.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "razorpay")
@Getter
@Setter
public class RazorpayProperties {

    private Key key = new Key();
    private Webhook webhook = new Webhook();
    private String mode = "test";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 10000;

    @Getter
    @Setter
    public static class Key {
        private String id;
        private String secret;
    }

    @Getter
    @Setter
    public static class Webhook {
        private String secret;
    }

    public String getKeyId() {
        return key != null ? key.getId() : null;
    }

    public String getKeySecret() {
        return key != null ? key.getSecret() : null;
    }

    public String getWebhookSecret() {
        return webhook != null ? webhook.getSecret() : null;
    }
}
