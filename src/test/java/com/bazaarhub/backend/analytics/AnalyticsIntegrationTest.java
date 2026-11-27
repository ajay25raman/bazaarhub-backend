package com.bazaarhub.backend.analytics;

import com.bazaarhub.backend.analytics.application.dto.request.LogAnalyticsEventRequest;
import com.bazaarhub.backend.analytics.domain.model.AnalyticsPlatform;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsIntegrationTest {

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

    private String customerToken;
    private String sellerToken;
    private String adminToken;
    private UserEntity customerUser;
    private UserEntity sellerUser;
    private UserEntity adminUser;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();

        RoleEntity customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_CUSTOMER").description("Customer").build()));
        RoleEntity sellerRole = roleRepository.findByName("ROLE_SELLER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_SELLER").description("Seller").build()));
        RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_ADMIN").description("Admin").build()));

        // Customer
        customerToken = registerAndLogin("an.customer@bazaarhub.com", "Password@123", "Analytics Customer", "+919876543301");
        customerUser = userRepository.findByEmail("an.customer@bazaarhub.com").orElseThrow();

        // Seller
        registerAndLogin("an.seller@bazaarhub.com", "Password@123", "Analytics Merchant", "+919876543302");
        sellerUser = userRepository.findByEmail("an.seller@bazaarhub.com").orElseThrow();
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", sellerUser.getId(), sellerRole.getId());
        sellerToken = login("an.seller@bazaarhub.com", "Password@123");

        SellerEntity seller = SellerEntity.builder()
                .user(sellerUser)
                .businessName("Analytics Merchant Store")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .verificationStatus(SellerVerificationStatus.VERIFIED)
                .build();
        sellerRepository.save(seller);

        // Admin
        registerAndLogin("an.admin@bazaarhub.com", "Password@123", "Analytics Admin", "+919876543303");
        adminUser = userRepository.findByEmail("an.admin@bazaarhub.com").orElseThrow();
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", adminUser.getId(), adminRole.getId());
        adminToken = login("an.admin@bazaarhub.com", "Password@123");
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
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
    @DisplayName("Public analytics event tracking records event without authentication")
    void testTrackPublicEvent() throws Exception {
        LogAnalyticsEventRequest request = LogAnalyticsEventRequest.builder()
                .sessionId("session-test-xyz")
                .eventName("PAGE_VIEW")
                .entityType("LANDING_PAGE")
                .platform(AnalyticsPlatform.WEB)
                .build();

        mockMvc.perform(post("/api/v1/public/analytics/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Event recorded")));
    }

    @Test
    @DisplayName("Admin overview analytics retrieves platform metrics")
    void testAdminOverviewAnalytics() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/overview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.grossMerchandiseValue", notNullValue()))
                .andExpect(jsonPath("$.data.totalOrders", notNullValue()));
    }

    @Test
    @DisplayName("Seller overview analytics retrieves seller-specific metrics")
    void testSellerOverviewAnalytics() throws Exception {
        mockMvc.perform(get("/api/v1/seller/analytics/overview")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.sellerId", notNullValue()))
                .andExpect(jsonPath("$.data.totalRevenue", notNullValue()));
    }

    @Test
    @DisplayName("Customer is forbidden from accessing Admin analytics")
    void testCustomerCannotAccessAdminAnalytics() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/overview")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }
}
