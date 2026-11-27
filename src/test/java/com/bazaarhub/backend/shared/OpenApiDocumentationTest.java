package com.bazaarhub.backend.shared;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Automated verification suite for OpenAPI 3 documentation.
 *
 * <p>
 * Verifies specification generation, documented operations, operation metadata,
 * response definitions, and security declarations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should successfully generate OpenAPI 3 spec with HTTP 200 OK")
    void shouldGenerateOpenApiDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should verify OpenAPI metadata and BearerAuth security scheme")
    void shouldVerifyOpenApiMetadataAndSecurityScheme() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());

        assertThat(root.has("openapi")).isTrue();
        assertThat(root.get("openapi").asText()).startsWith("3.");

        JsonNode info = root.get("info");
        assertThat(info).isNotNull();
        assertThat(info.get("title").asText()).isEqualTo("BazaarHub REST API");
        assertThat(info.get("version").asText()).isEqualTo("1.0.0");

        JsonNode components = root.get("components");
        assertThat(components).isNotNull();

        JsonNode securitySchemes = components.get("securitySchemes");
        assertThat(securitySchemes).isNotNull();
        assertThat(securitySchemes.has(OpenApiConfig.SECURITY_SCHEME_NAME)).isTrue();

        JsonNode bearerScheme = securitySchemes.get(OpenApiConfig.SECURITY_SCHEME_NAME);
        assertThat(bearerScheme.get("type").asText().toLowerCase()).isEqualTo("http");
        assertThat(bearerScheme.get("scheme").asText().toLowerCase()).isEqualTo("bearer");
        assertThat(bearerScheme.get("bearerFormat").asText()).isEqualTo("JWT");
    }

    @Test
    @DisplayName("Should verify total documented operations with operation metadata and responses")
    void shouldVerifyAll286EndpointsDocumented() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode paths = root.get("paths");
        assertThat(paths).isNotNull();

        int totalOperations = 0;
        Set<String> operationIds = new HashSet<>();
        List<String> httpMethods = List.of("get", "post", "put", "delete", "patch");

        Iterator<Map.Entry<String, JsonNode>> pathFields = paths.properties().iterator();
        while (pathFields.hasNext()) {
            Map.Entry<String, JsonNode> pathEntry = pathFields.next();
            String path = pathEntry.getKey();
            JsonNode pathItem = pathEntry.getValue();

            for (String method : httpMethods) {
                if (pathItem.has(method)) {
                    totalOperations++;
                    JsonNode operation = pathItem.get(method);

                    assertThat(operation.has("summary"))
                            .as("Path " + method.toUpperCase() + " " + path + " must have a summary")
                            .isTrue();
                    assertThat(operation.get("summary").asText()).isNotBlank();

                    assertThat(operation.has("responses"))
                            .as("Path " + method.toUpperCase() + " " + path + " must have responses")
                            .isTrue();
                    JsonNode responses = operation.get("responses");
                    assertThat(responses.size())
                            .as("Path " + method.toUpperCase() + " " + path + " must have at least one response")
                            .isGreaterThan(0);

                    if (operation.has("operationId")) {
                        String opId = operation.get("operationId").asText();
                        assertThat(operationIds.add(opId))
                                .as("Operation ID '" + opId + "' must be unique: duplicate found on "
                                        + method.toUpperCase() + " " + path)
                                .isTrue();
                    }
                }
            }
        }

        assertThat(totalOperations)
                .as("Total documented REST operations (286 baseline methods + 6 path aliases)")
                .isEqualTo(292);
    }

    @Test
    @DisplayName("Should verify secured vs public endpoint security specifications")
    void shouldVerifySecuredVsPublicEndpointSecurity() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode paths = root.get("paths");
        assertThat(paths).isNotNull();

        JsonNode healthPath = paths.get("/api/v1/health");
        assertThat(healthPath).isNotNull();
        JsonNode healthGet = healthPath.get("get");
        assertThat(healthGet).isNotNull();
        assertThat(healthGet.has("security")).isFalse();

        JsonNode loginPath = paths.get("/api/v1/auth/login");
        assertThat(loginPath).isNotNull();
        JsonNode loginPost = loginPath.get("post");
        assertThat(loginPost).isNotNull();
        if (loginPost.has("security")) {
            assertThat(loginPost.get("security").isEmpty()).isTrue();
        }

        JsonNode sellerProfilePath = paths.get("/api/v1/sellers/me");
        assertThat(sellerProfilePath).isNotNull();
        JsonNode sellerProfileGet = sellerProfilePath.get("get");
        assertThat(sellerProfileGet).isNotNull();
        assertThat(sellerProfileGet.has("security")).isTrue();
        boolean hasBearer = false;
        for (JsonNode requirement : sellerProfileGet.get("security")) {
            if (requirement.has(OpenApiConfig.SECURITY_SCHEME_NAME)) {
                hasBearer = true;
                break;
            }
        }
        assertThat(hasBearer)
                .as("GET /api/v1/sellers/me must require bearerAuth security")
                .isTrue();

        JsonNode adminSellersPath = paths.get("/api/v1/admin/sellers");
        assertThat(adminSellersPath).isNotNull();
        JsonNode adminSellersGet = adminSellersPath.get("get");
        assertThat(adminSellersGet).isNotNull();
        assertThat(adminSellersGet.has("security")).isTrue();
        hasBearer = false;
        for (JsonNode requirement : adminSellersGet.get("security")) {
            if (requirement.has(OpenApiConfig.SECURITY_SCHEME_NAME)) {
                hasBearer = true;
                break;
            }
        }
        assertThat(hasBearer)
                .as("GET /api/v1/admin/sellers must require bearerAuth security")
                .isTrue();
    }
}