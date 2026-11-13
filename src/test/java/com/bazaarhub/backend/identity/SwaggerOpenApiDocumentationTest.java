package com.bazaarhub.backend.identity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SwaggerOpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /v3/api-docs - OpenAPI 3 JSON is publicly available with correct metadata")
    void testOpenApiJsonAvailabilityAndMetadata() throws Exception {
        mockMvc.perform(get("/v3/api-docs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi", startsWith("3.")))
                .andExpect(jsonPath("$.info.title", is("BazaarHub REST API")))
                .andExpect(jsonPath("$.info.version", is("1.0.0")))
                .andExpect(jsonPath("$.info.description", containsString("Enterprise Multi-Vendor Marketplace")))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type", is("http")))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme", is("bearer")))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat", is("JWT")))
                .andExpect(jsonPath("$.paths['/api/v1/auth/register']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/auth/login']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/auth/me']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/auth/logout']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/categories']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/categories/tree']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/categories/root']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/brands']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/admin/categories']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/admin/brands']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/auth/me'].get.security[0].bearerAuth", notNullValue()));
    }

    @Test
    @DisplayName("GET /swagger-ui/index.html - Swagger UI endpoint is accessible without authentication")
    void testSwaggerUiAvailability() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(status == 200 || (status >= 300 && status < 400),
                            "Expected 200 OK or 3xx redirection, but got: " + status);
                });
    }

    @Test
    @DisplayName("GET /actuator/health - Health check endpoint is accessible without authentication")
    void testActuatorHealthPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }
}
