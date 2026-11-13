package com.bazaarhub.backend.seller;

import com.bazaarhub.backend.identity.application.dto.LoginRequest;
import com.bazaarhub.backend.identity.application.dto.RegisterRequest;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.PermissionEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.RoleEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataPermissionRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataRoleRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserDeviceRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.seller.application.dto.request.*;
import com.bazaarhub.backend.seller.domain.model.DocumentVerificationStatus;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.domain.model.StoreStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.StoreEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.*;
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

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SellerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataUserDeviceRepository userDeviceRepository;

    @Autowired
    private SpringDataRoleRepository roleRepository;

    @Autowired
    private SpringDataPermissionRepository permissionRepository;

    @Autowired
    private SpringDataSellerRepository sellerRepository;

    @Autowired
    private SpringDataStoreRepository storeRepository;

    @Autowired
    private SpringDataSellerDocumentRepository documentRepository;

    @Autowired
    private SpringDataSellerBankAccountRepository bankAccountRepository;

    private String customerToken;
    private Long customerUserId;

    private String adminToken;
    private Long adminUserId;

    private String secondCustomerToken;
    private Long secondCustomerUserId;

    @BeforeEach
    void setUp() throws Exception {
        // Clear previous state in referential integrity order
        bankAccountRepository.deleteAll();
        documentRepository.deleteAll();
        storeRepository.deleteAll();
        sellerRepository.deleteAll();
        userDeviceRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Ensure basic roles exist
        RoleEntity customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_CUSTOMER")
                        .description("Default customer role")
                        .build()));

        RoleEntity sellerRole = roleRepository.findByName("ROLE_SELLER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_SELLER")
                        .description("Seller role")
                        .build()));

        PermissionEntity sellerManagePerm = permissionRepository.findByName("SELLER_MANAGE")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder()
                        .name("SELLER_MANAGE")
                        .description("Manage sellers")
                        .build()));

        RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_ADMIN")
                        .description("Administrator role")
                        .permissions(Set.of(sellerManagePerm))
                        .build()));

        // 2. Register & Login Customer Alice
        customerToken = registerAndLogin("Alice Customer", "alice@customer.com", "+919876543210", "Password123!");
        customerUserId = userRepository.findByEmail("alice@customer.com").orElseThrow().getId();

        // 3. Register & Login Customer Bob (second customer)
        secondCustomerToken = registerAndLogin("Bob Customer", "bob@customer.com", "+919876543211", "Password123!");
        secondCustomerUserId = userRepository.findByEmail("bob@customer.com").orElseThrow().getId();

        // 4. Register & Login Admin
        registerAndLogin("Super Admin", "admin@bazaarhub.com", "+919876543299", "AdminPass123!");
        UserEntity adminUser = userRepository.findByIdentifierWithRolesAndPermissions("admin@bazaarhub.com").orElseThrow();
        adminUser.getRoles().add(adminRole);
        userRepository.save(adminUser);

        adminToken = login("admin@bazaarhub.com", "AdminPass123!");
        adminUserId = adminUser.getId();
    }

    private String registerAndLogin(String name, String email, String phone, String password) throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .fullName(name)
                .email(email)
                .phone(phone)
                .password(password)
                .confirmPassword(password)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        return login(email, password);
    }

    private String login(String identifier, String password) throws Exception {
        LoginRequest loginReq = LoginRequest.builder()
                .identifier(identifier)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("End-to-End Seller Onboarding Lifecycle: Onboard, Documents, Store, Bank, and Admin Approval")
    void testFullSellerOnboardingAndAdminApprovalLifecycle() throws Exception {
        // Step 1: Submit Onboarding Application as Customer Alice
        SellerOnboardingRequest onboardingReq = SellerOnboardingRequest.builder()
                .businessName("Wonderland Emporium")
                .businessEmail("contact@wonderland.com")
                .businessPhone("+919876543210")
                .taxIdentifier("29WOND1234F1Z5")
                .build();

        MvcResult onboardResult = mockMvc.perform(post("/api/v1/sellers/onboarding")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(onboardingReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.businessName", is("Wonderland Emporium")))
                .andExpect(jsonPath("$.data.approvalStatus", is("PENDING")))
                .andExpect(jsonPath("$.data.verificationStatus", is("PENDING")))
                .andReturn();

        JsonNode onboardData = objectMapper.readTree(onboardResult.getResponse().getContentAsString()).path("data");
        Long sellerId = onboardData.path("id").asLong();

        // Step 2: Duplicate Onboarding Attempt Must Fail with 409 Conflict
        mockMvc.perform(post("/api/v1/sellers/onboarding")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(onboardingReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already exists")));

        // Step 3: Fetch Seller Profile & Update Profile
        mockMvc.perform(get("/api/v1/sellers/me")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.businessName", is("Wonderland Emporium")));

        UpdateSellerProfileRequest updateReq = UpdateSellerProfileRequest.builder()
                .businessName("Wonderland Emporium Private Limited")
                .build();

        mockMvc.perform(put("/api/v1/sellers/me")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.businessName", is("Wonderland Emporium Private Limited")));

        // Step 4: Submit KYC Document
        SellerDocumentRequest docReq = SellerDocumentRequest.builder()
                .documentType("GST_CERTIFICATE")
                .documentNumber("29WOND1234F1Z5")
                .documentUrl("https://storage.bazaarhub.com/kyc/alice-gst.pdf")
                .build();

        MvcResult docResult = mockMvc.perform(post("/api/v1/sellers/me/documents")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(docReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.documentType", is("GST_CERTIFICATE")))
                .andExpect(jsonPath("$.data.verificationStatus", is("PENDING")))
                .andReturn();

        Long documentId = objectMapper.readTree(docResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Step 5: Test Resource Ownership - Bob must NOT be able to view Alice's document
        mockMvc.perform(get("/api/v1/sellers/me/documents/" + documentId)
                        .header("Authorization", "Bearer " + secondCustomerToken))
                .andExpect(status().isNotFound()); // Bob has no seller profile -> 404

        // Step 6: Create Storefront (Initial status INACTIVE)
        StoreCreateRequest storeReq = StoreCreateRequest.builder()
                .storeName("Wonderland Official")
                .slug("wonderland-official")
                .description("Everything whimsical and electronic")
                .build();

        mockMvc.perform(post("/api/v1/sellers/me/store")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storeReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.storeName", is("Wonderland Official")))
                .andExpect(jsonPath("$.data.status", is("INACTIVE")));

        // Step 7: Submit Settlement Bank Account (Encrypted at rest, masked in response)
        SellerBankRequest bankReq = SellerBankRequest.builder()
                .accountHolderName("Wonderland Emporium Pvt Ltd")
                .accountNumber("50100456789012")
                .ifscCode("HDFC0001234")
                .bankName("HDFC Bank")
                .branchName("MG Road")
                .isPrimary(true)
                .build();

        mockMvc.perform(post("/api/v1/sellers/me/bank")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bankReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.maskedAccountNumber", is("XXXXXXXXXX9012")))
                .andExpect(jsonPath("$.data.maskedIfscCode", is("HDFC****234")))
                .andExpect(jsonPath("$.data.isPrimary", is(true)));

        // Step 8: Regular customer cannot access Admin endpoints -> 403 Forbidden
        mockMvc.perform(get("/api/v1/admin/sellers")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // Step 9: Admin lists sellers and retrieves application dossier
        mockMvc.perform(get("/api/v1/admin/sellers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/api/v1/admin/sellers/" + sellerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(sellerId.intValue())))
                .andExpect(jsonPath("$.data.documents", hasSize(1)))
                .andExpect(jsonPath("$.data.bankAccounts", hasSize(1)));

        // Step 10: Admin verifies KYC Document
        VerifyDocumentRequest verifyDocReq = VerifyDocumentRequest.builder()
                .documentId(documentId)
                .status(DocumentVerificationStatus.VERIFIED)
                .build();

        mockMvc.perform(post("/api/v1/admin/sellers/" + sellerId + "/verify-documents")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyDocReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationStatus", is("VERIFIED")));

        // Step 11: Admin approves Seller Application
        mockMvc.perform(post("/api/v1/admin/sellers/" + sellerId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalStatus", is("APPROVED")));

        // Step 12: Verify User received ROLE_SELLER while preserving ROLE_CUSTOMER
        UserEntity updatedAlice = userRepository.findByIdWithRolesAndPermissions(customerUserId).orElseThrow();
        Set<String> aliceRoles = new HashSet<>();
        updatedAlice.getRoles().forEach(r -> aliceRoles.add(r.getName()));

        assertTrue(aliceRoles.contains("ROLE_SELLER"), "Alice must now hold ROLE_SELLER");
        assertTrue(aliceRoles.contains("ROLE_CUSTOMER"), "Alice must still hold ROLE_CUSTOMER");

        // Verify Storefront is now ACTIVE
        StoreEntity aliceStore = storeRepository.findBySellerId(sellerId).orElseThrow();
        assertEquals(StoreStatus.ACTIVE, aliceStore.getStatus());

        // Step 13: Admin suspends Seller
        AdminActionRequest suspendReq = AdminActionRequest.builder()
                .reason("Compliance review suspension")
                .build();

        mockMvc.perform(post("/api/v1/admin/sellers/" + sellerId + "/suspend")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(suspendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalStatus", is("SUSPENDED")));

        // Verify Storefront is now SUSPENDED
        aliceStore = storeRepository.findBySellerId(sellerId).orElseThrow();
        assertEquals(StoreStatus.SUSPENDED, aliceStore.getStatus());
    }

    @Test
    @DisplayName("Admin cannot approve their own seller application")
    void testAdminSelfApprovalForbidden() throws Exception {
        // Admin submits their own seller application
        SellerOnboardingRequest onboardingReq = SellerOnboardingRequest.builder()
                .businessName("Admin Side Business")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/sellers/onboarding")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(onboardingReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long adminSellerId = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Admin attempts to approve their own seller application -> 422 Unprocessable Content / Bad Request
        mockMvc.perform(post("/api/v1/admin/sellers/" + adminSellerId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("cannot approve their own")));
    }
}
