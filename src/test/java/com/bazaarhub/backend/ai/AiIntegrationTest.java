package com.bazaarhub.backend.ai;

import com.bazaarhub.backend.ai.application.dto.request.AiChatRequest;
import com.bazaarhub.backend.ai.application.dto.request.AiGenerateDescriptionRequest;
import com.bazaarhub.backend.ai.infrastructure.client.AiClient;
import com.bazaarhub.backend.ai.infrastructure.client.AiCompletionResponse;
import com.bazaarhub.backend.identity.application.dto.LoginRequest;
import com.bazaarhub.backend.identity.application.dto.RegisterRequest;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.RoleEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataRoleRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.domain.model.SellerVerificationStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataRoleRepository roleRepository;

    @Autowired
    private SpringDataSellerRepository sellerRepository;

    @MockitoBean
    private AiClient aiClient;

    private String customerToken;
    private String sellerToken;
    private String adminToken;
    private UserEntity customerUser;
    private UserEntity sellerUser;
    private UserEntity adminUser;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();

        when(aiClient.isEnabled()).thenReturn(true);
        when(aiClient.getProviderName()).thenReturn("Groq-Mock");
        when(aiClient.getModelName()).thenReturn("openai/gpt-oss-20b");

        RoleEntity customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_CUSTOMER").description("Customer").build()));
        RoleEntity sellerRole = roleRepository.findByName("ROLE_SELLER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_SELLER").description("Seller").build()));
        RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_ADMIN").description("Admin").build()));

        // Customer
        customerToken = registerAndLogin("ai.customer@bazaarhub.com", "Password@123", "AI Customer", "+919876543201");
        customerUser = userRepository.findByEmail("ai.customer@bazaarhub.com").orElseThrow();

        // Seller
        registerAndLogin("ai.seller@bazaarhub.com", "Password@123", "AI Merchant", "+919876543202");
        sellerUser = userRepository.findByEmail("ai.seller@bazaarhub.com").orElseThrow();
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", sellerUser.getId(), sellerRole.getId());
        sellerToken = login("ai.seller@bazaarhub.com", "Password@123");

        SellerEntity seller = SellerEntity.builder()
                .user(sellerUser)
                .businessName("AI Tech Store")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .verificationStatus(SellerVerificationStatus.VERIFIED)
                .build();
        sellerRepository.save(seller);

        // Admin
        registerAndLogin("ai.admin@bazaarhub.com", "Password@123", "AI Admin", "+919876543203");
        adminUser = userRepository.findByEmail("ai.admin@bazaarhub.com").orElseThrow();
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", adminUser.getId(), adminRole.getId());
        adminToken = login("ai.admin@bazaarhub.com", "Password@123");
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("DELETE FROM ai_messages");
        jdbcTemplate.execute("DELETE FROM ai_conversations");
        jdbcTemplate.execute("DELETE FROM ai_usage_logs");
        jdbcTemplate.execute("DELETE FROM analytics_events");
        jdbcTemplate.execute("DELETE FROM sellers");
        jdbcTemplate.execute("DELETE FROM user_roles");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    private String registerAndLogin(String email, String password, String fullName, String phone) throws Exception {
        RegisterRequest register = RegisterRequest.builder()
                .email(email)
                .password(password)
                .confirmPassword(password)
                .fullName(fullName)
                .phone(phone)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        return login(email, password);
    }

    private String login(String email, String password) throws Exception {
        LoginRequest login = LoginRequest.builder()
                .identifier(email)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Public AI Chat endpoint answers marketplace queries without credentials")
    void testPublicAiChat() throws Exception {
        when(aiClient.completeChat(anyList(), anyInt(), anyDouble()))
                .thenReturn(AiCompletionResponse.builder()
                        .content("BazaarHub offers free returns within 30 days.")
                        .model("openai/gpt-oss-20b")
                        .promptTokens(80)
                        .completionTokens(20)
                        .totalTokens(100)
                        .latencyMs(120)
                        .build());

        AiChatRequest request = AiChatRequest.builder()
                .message("What is BazaarHub's return policy?")
                .build();

        mockMvc.perform(post("/api/v1/public/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.reply", containsString("30 days")));
    }

    @Test
    @DisplayName("Admin AI status returns active provider and model")
    void testAdminAiStatus() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ai/status")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.provider", is("Groq-Mock")))
                .andExpect(jsonPath("$.data.model", is("openai/gpt-oss-20b")));
    }

    @Test
    @DisplayName("Seller AI description generates draft product listing")
    void testSellerAiGenerateDescription() throws Exception {
        String draftJson = """
                {
                  "suggestedTitles": ["Smart Bluetooth Speaker V2"],
                  "shortDescription": "Crisp 360 audio with 24-hour battery.",
                  "longDescription": "Experience immersive room-filling sound with deep bass.",
                  "features": ["Waterproof IPX7", "Bluetooth 5.3"],
                  "keywords": ["speaker", "audio", "bluetooth"]
                }
                """;

        when(aiClient.completeChat(anyList(), anyInt(), anyDouble()))
                .thenReturn(AiCompletionResponse.builder()
                        .content(draftJson)
                        .model("openai/gpt-oss-20b")
                        .promptTokens(150)
                        .completionTokens(100)
                        .totalTokens(250)
                        .latencyMs(200)
                        .build());

        AiGenerateDescriptionRequest request = AiGenerateDescriptionRequest.builder()
                .productName("Smart Bluetooth Speaker")
                .categoryName("Electronics")
                .keyFeatures(List.of("Waterproof", "24-hour battery"))
                .targetAudience("Energetic music lovers")
                .build();

        mockMvc.perform(post("/api/v1/seller/ai/generate-description")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("DRAFT")))
                .andExpect(jsonPath("$.data.suggestedTitles[0]", is("Smart Bluetooth Speaker V2")));
    }
}
