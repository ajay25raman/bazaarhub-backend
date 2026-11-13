package com.bazaarhub.backend.bootstrap.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 / Swagger configuration for BazaarHub REST API.
 */
@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BazaarHub REST API")
                        .description("Enterprise Multi-Vendor Marketplace Backend")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("BazaarHub Engineering Team")
                                .email("engineering@bazaarhub.com")
                                .url("https://bazaarhub.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://bazaarhub.com/license")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local Development Server")
                ))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecuritySchemeType.HTTP.toOpenApiScheme())
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Provide a signed JWT Bearer access token to authorize requests. Format: Bearer <token>")
                        ));
    }

    private enum SecuritySchemeType {
        HTTP;

        public SecurityScheme.Type toOpenApiScheme() {
            return SecurityScheme.Type.HTTP;
        }
    }
}
