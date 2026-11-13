package com.bazaarhub.backend.identity;

import com.bazaarhub.backend.identity.application.dto.LoginRequest;
import com.bazaarhub.backend.identity.application.dto.RegisterRequest;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserDeviceRepository userDeviceRepository;

    @Autowired
    private com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataRoleRepository roleRepository;

    @Autowired(required = false)
    private com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerBankAccountRepository bankAccountRepository;

    @Autowired(required = false)
    private com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerDocumentRepository documentRepository;

    @Autowired(required = false)
    private com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataStoreRepository storeRepository;

    @Autowired(required = false)
    private com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository sellerRepository;

    @Autowired(required = false)
    private com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataCategoryRepository categoryRepository;

    @Autowired(required = false)
    private com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataBrandRepository brandRepository;

    @BeforeEach
    void cleanUp() {
        if (categoryRepository != null) categoryRepository.deleteAll();
        if (brandRepository != null) brandRepository.deleteAll();
        if (bankAccountRepository != null) bankAccountRepository.deleteAll();
        if (documentRepository != null) documentRepository.deleteAll();
        if (storeRepository != null) storeRepository.deleteAll();
        if (sellerRepository != null) sellerRepository.deleteAll();
        userDeviceRepository.deleteAll();
        userRepository.deleteAll();
        if (roleRepository.findByName("ROLE_CUSTOMER").isEmpty()) {
            roleRepository.save(com.bazaarhub.backend.identity.infrastructure.persistence.entity.RoleEntity.builder()
                    .name("ROLE_CUSTOMER")
                    .description("Default marketplace customer role")
                    .build());
        }
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Register new user successfully")
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Alice Wonderland")
                .email("alice@example.com")
                .phone("+919123456789")
                .password("Password123!")
                .confirmPassword("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("registered successfully")))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.fullName", is("Alice Wonderland")))
                .andExpect(jsonPath("$.data.email", is("alice@example.com")))
                .andExpect(jsonPath("$.data.accountStatus", is("ACTIVE")))
                .andExpect(jsonPath("$.data.roles", hasItem("ROLE_CUSTOMER")))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist()) // Never return password hashes
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        // Verify registered customer receives the correct user_roles mapping in database
        var registeredUser = userRepository.findByIdentifierWithRolesAndPermissions("alice@example.com");
        org.junit.jupiter.api.Assertions.assertTrue(registeredUser.isPresent(), "User must be persisted in database");
        org.junit.jupiter.api.Assertions.assertTrue(
                registeredUser.get().getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_CUSTOMER")),
                "User must be mapped to ROLE_CUSTOMER in user_roles join table"
        );
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Duplicate registration returns 409 Conflict")
    void testRegisterDuplicate() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Bob Builder")
                .email("bob@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Attempt duplicate registration
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Registration fails safely when ROLE_CUSTOMER is missing")
    void testRegisterFailsSafelyWhenRoleCustomerMissing() throws Exception {
        roleRepository.deleteAll(); // Remove role to test failure safety

        RegisterRequest request = RegisterRequest.builder()
                .fullName("Charlie Brown")
                .email("charlie@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("ROLE_CUSTOMER")));

        // Verify no user was created in the database
        org.junit.jupiter.api.Assertions.assertFalse(
                userRepository.existsByEmail("charlie@example.com"),
                "User must not be persisted when default customer role is missing"
        );
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Invalid input returns 400 Bad Request with validation errors")
    void testRegisterValidationFailure() throws Exception {
        RegisterRequest invalidRequest = RegisterRequest.builder()
                .fullName("") // Blank name
                .email("not-an-email") // Bad email format
                .password("short") // Less than 8 characters
                .confirmPassword("mismatch")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errors", not(empty())));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Valid login returns JWT token and user profile")
    void testLoginSuccess() throws Exception {
        // First register
        RegisterRequest registerReq = RegisterRequest.builder()
                .fullName("Charlie Brown")
                .email("charlie@example.com")
                .password("SecretPassword123!")
                .confirmPassword("SecretPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Then login
        LoginRequest loginReq = LoginRequest.builder()
                .identifier("charlie@example.com")
                .password("SecretPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Login successful")))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.expiresIn", greaterThan(0)))
                .andExpect(jsonPath("$.data.user.email", is("charlie@example.com")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Invalid password returns 401 Unauthorized")
    void testLoginInvalidPassword() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .fullName("David Copperfield")
                .email("david@example.com")
                .password("RealPass123!")
                .confirmPassword("RealPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = LoginRequest.builder()
                .identifier("david@example.com")
                .password("WrongPassword999!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Invalid email/phone or password")));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Access protected endpoint with valid JWT")
    void testGetMeProtectedEndpoint() throws Exception {
        // Register
        RegisterRequest registerReq = RegisterRequest.builder()
                .fullName("Emma Watson")
                .email("emma@example.com")
                .phone("+919876543211")
                .password("EmmaPass123!")
                .confirmPassword("EmmaPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Login to get token
        LoginRequest loginReq = LoginRequest.builder()
                .identifier("emma@example.com")
                .password("EmmaPass123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode rootNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = rootNode.path("data").path("accessToken").asText();
        assertNotNull(token);

        // Access /api/v1/auth/me with Bearer token
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fullName", is("Emma Watson")))
                .andExpect(jsonPath("$.data.email", is("emma@example.com")))
                .andExpect(jsonPath("$.data.phone", is("+919876543211")));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Accessing without token returns 401 Unauthorized")
    void testGetMeWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Unauthorized")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - Logout with token returns 200 OK")
    void testLogoutSuccess() throws Exception {
        // Register & Login
        RegisterRequest registerReq = RegisterRequest.builder()
                .fullName("Frank Ocean")
                .email("frank@example.com")
                .password("FrankPass123!")
                .confirmPassword("FrankPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = LoginRequest.builder()
                .identifier("frank@example.com")
                .password("FrankPass123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token)
                        .param("deviceId", "device-test-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Logout successful")));
    }

    @Test
    @DisplayName("Device revocation: Token with deactivated device session is rejected with 401")
    void testDeviceRevocationAfterLogout() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .fullName("Grace Hopper")
                .email("grace@example.com")
                .password("GracePass123!")
                .confirmPassword("GracePass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Login with device session
        LoginRequest loginReq = LoginRequest.builder()
                .identifier("grace@example.com")
                .password("GracePass123!")
                .deviceInfo(com.bazaarhub.backend.identity.application.dto.DeviceInfoRequest.builder()
                        .deviceId("pixel-revoke-test-123")
                        .deviceName("Google Pixel 9")
                        .deviceType(com.bazaarhub.backend.identity.domain.model.DeviceType.ANDROID)
                        .build())
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // 1. Initial request with active device token succeeds
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email", is("grace@example.com")));

        // 2. Logout and revoke the device session
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token)
                        .param("deviceId", "pixel-revoke-test-123"))
                .andExpect(status().isOk());

        // 3. Subsequent request with the same token is rejected because the device session was revoked
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Returns 501 Not Implemented explaining schema limitation")
    void testRefreshTokenReturns501() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"test-refresh-token\"}"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("refresh_tokens")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/forgot-password - Returns 501 Not Implemented")
    void testForgotPasswordReturns501() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"user@example.com\"}"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
