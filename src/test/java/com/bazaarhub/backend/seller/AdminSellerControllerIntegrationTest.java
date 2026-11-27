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
import com.bazaarhub.backend.seller.application.dto.request.AdminActionRequest;
import com.bazaarhub.backend.seller.application.dto.request.VerifyDocumentRequest;
import com.bazaarhub.backend.seller.domain.model.DocumentVerificationStatus;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.domain.model.SellerVerificationStatus;
import com.bazaarhub.backend.seller.domain.model.StoreStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerDocumentEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.StoreEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerBankAccountRepository;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerDocumentRepository;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataStoreRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Test suite for AdminSellerController.
 * Validates seller application listing, dossier retrieval, document KYC verification,
 * approval, rejection, suspension, and role-based access control.
 * Stage 5 REM-TEST-01 verification.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSellerControllerIntegrationTest {

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private Long adminUserId;

    private String customerToken;

    private Long pendingSellerId;
    private Long pendingSellerDocId;
    private Long pendingSellerStoreId;

    private Long adminAsSellerId;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.execute("DELETE FROM delivery_tracking");
        jdbcTemplate.execute("DELETE FROM delivery_assignments");
        jdbcTemplate.execute("DELETE FROM shipments");
        jdbcTemplate.execute("DELETE FROM delivery_boys");
        jdbcTemplate.execute("DELETE FROM refunds");
        jdbcTemplate.execute("DELETE FROM payment_transactions");
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM order_status_history");
        jdbcTemplate.execute("DELETE FROM order_items");
        jdbcTemplate.execute("DELETE FROM seller_orders");
        jdbcTemplate.execute("DELETE FROM orders");
        jdbcTemplate.execute("DELETE FROM cart_items");
        jdbcTemplate.execute("DELETE FROM carts");
        jdbcTemplate.execute("DELETE FROM wishlist_items");
        jdbcTemplate.execute("DELETE FROM wishlists");
        jdbcTemplate.execute("DELETE FROM addresses");
        jdbcTemplate.execute("DELETE FROM customer_profiles");
        jdbcTemplate.execute("DELETE FROM inventory_movements");
        jdbcTemplate.execute("DELETE FROM stock_reservations");
        jdbcTemplate.execute("DELETE FROM inventories");
        jdbcTemplate.execute("DELETE FROM product_variants");
        jdbcTemplate.execute("DELETE FROM product_images");
        jdbcTemplate.execute("DELETE FROM products");
        bankAccountRepository.deleteAll();
        documentRepository.deleteAll();
        storeRepository.deleteAll();
        sellerRepository.deleteAll();
        jdbcTemplate.execute("DELETE FROM user_roles");
        userDeviceRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Roles & Permissions setup
        PermissionEntity sellerManagePerm = permissionRepository.findByName("SELLER_MANAGE")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder()
                        .name("SELLER_MANAGE")
                        .description("Manage sellers")
                        .build()));

        RoleEntity customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_CUSTOMER")
                        .description("Customer")
                        .build()));

        RoleEntity sellerRole = roleRepository.findByName("ROLE_SELLER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_SELLER")
                        .description("Seller")
                        .build()));

        RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_ADMIN")
                        .description("Administrator")
                        .permissions(Set.of(sellerManagePerm))
                        .build()));

        // 2. Register & setup Admin
        registerAndLogin("Super Admin", "admin.sellers@bazaarhub.com", "+919200000001", "AdminPass123!");
        UserEntity adminUser = userRepository.findByIdentifierWithRolesAndPermissions("admin.sellers@bazaarhub.com").orElseThrow();
        adminUser.getRoles().add(adminRole);
        userRepository.save(adminUser);
        adminToken = login("admin.sellers@bazaarhub.com", "AdminPass123!");
        adminUserId = adminUser.getId();

        // 3. Register Customer
        customerToken = registerAndLogin("Regular Customer", "customer.sellers@bazaarhub.com", "+919200000002", "CustomerPass123!");

        // 4. Register Candidate Seller
        registerAndLogin("Candidate Merchant", "candidate.merchant@bazaarhub.com", "+919200000003", "MerchantPass123!");
        UserEntity merchantUser = userRepository.findByIdentifierWithRolesAndPermissions("candidate.merchant@bazaarhub.com").orElseThrow();

        SellerEntity candidateSeller = sellerRepository.save(SellerEntity.builder()
                .user(merchantUser)
                .businessName("Candidate Traders")
                .businessEmail("contact@candidatetraders.com")
                .businessPhone("+919200000003")
                .taxIdentifier("29ABCDE1234F1Z5")
                .approvalStatus(SellerApprovalStatus.PENDING)
                .verificationStatus(SellerVerificationStatus.PENDING)
                .build());
        pendingSellerId = candidateSeller.getId();

        StoreEntity candidateStore = storeRepository.save(StoreEntity.builder()
                .seller(candidateSeller)
                .storeName("Candidate Megastore")
                .slug("candidate-megastore")
                .description("Everything under one roof")
                .status(StoreStatus.INACTIVE)
                .build());
        pendingSellerStoreId = candidateStore.getId();
        candidateSeller.setStore(candidateStore);
        sellerRepository.save(candidateSeller);

        SellerDocumentEntity doc = documentRepository.save(SellerDocumentEntity.builder()
                .seller(candidateSeller)
                .documentType("GSTIN_CERTIFICATE")
                .documentUrl("https://cloudinary.com/docs/gstin.pdf")
                .verificationStatus(DocumentVerificationStatus.PENDING)
                .build());
        pendingSellerDocId = doc.getId();

        // 5. Create Seller profile owned by Admin for self-approval test
        SellerEntity adminSeller = sellerRepository.save(SellerEntity.builder()
                .user(adminUser)
                .businessName("Admin Side Business")
                .businessEmail("admin.business@bazaarhub.com")
                .businessPhone("+919200000001")
                .approvalStatus(SellerApprovalStatus.PENDING)
                .verificationStatus(SellerVerificationStatus.PENDING)
                .build());
        adminAsSellerId = adminSeller.getId();
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
    @DisplayName("Admin can list seller applications with optional status filter")
    void listSellers_success() throws Exception {
        // List all
        mockMvc.perform(get("/api/v1/admin/sellers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(2))));

        // Filter by PENDING approval
        mockMvc.perform(get("/api/v1/admin/sellers")
                        .param("approvalStatus", "PENDING")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].approvalStatus").value("PENDING"));
    }

    @Test
    @DisplayName("Admin can view full seller dossier details")
    void getSellerDetails_success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sellers/{sellerId}", pendingSellerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(pendingSellerId))
                .andExpect(jsonPath("$.data.businessName").value("Candidate Traders"))
                .andExpect(jsonPath("$.data.documents", hasSize(1)))
                .andExpect(jsonPath("$.data.documents[0].documentType").value("GSTIN_CERTIFICATE"));
    }

    @Test
    @DisplayName("Non-existent seller ID returns 404 Not Found")
    void getSellerDetails_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sellers/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Admin can review and verify KYC document (promotes seller to VERIFIED when all documents verified)")
    void verifyDocument_approved() throws Exception {
        VerifyDocumentRequest request = VerifyDocumentRequest.builder()
                .documentId(pendingSellerDocId)
                .status(DocumentVerificationStatus.VERIFIED)
                .build();

        mockMvc.perform(post("/api/v1/admin/sellers/{sellerId}/verify-documents", pendingSellerId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"));

        // Seller verification status should also be updated to VERIFIED
        mockMvc.perform(get("/api/v1/admin/sellers/{sellerId}", pendingSellerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"));
    }

    @Test
    @DisplayName("Admin can reject KYC document with reason")
    void verifyDocument_rejected() throws Exception {
        VerifyDocumentRequest request = VerifyDocumentRequest.builder()
                .documentId(pendingSellerDocId)
                .status(DocumentVerificationStatus.REJECTED)
                .rejectionReason("GSTIN certificate is blurry and unreadable")
                .build();

        mockMvc.perform(post("/api/v1/admin/sellers/{sellerId}/verify-documents", pendingSellerId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verificationStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").value("GSTIN certificate is blurry and unreadable"));
    }

    @Test
    @DisplayName("Admin approving seller grants ROLE_SELLER and activates storefront")
    void approveSeller_success() throws Exception {
        mockMvc.perform(post("/api/v1/admin/sellers/{sellerId}/approve", pendingSellerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.approvalStatus").value("APPROVED"));

        // Verify storefront status is now ACTIVE
        StoreEntity store = storeRepository.findById(pendingSellerStoreId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(StoreStatus.ACTIVE, store.getStatus());
    }

    @Test
    @DisplayName("Admin cannot self-approve their own seller application -> 422 Unprocessable Entity")
    void approveSeller_selfApprovalForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/sellers/{sellerId}/approve", adminAsSellerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Admin can reject seller application with required reason")
    void rejectSeller_success() throws Exception {
        AdminActionRequest request = AdminActionRequest.builder()
                .reason("Failed compliance check: Business registration invalid")
                .build();

        mockMvc.perform(post("/api/v1/admin/sellers/{sellerId}/reject", pendingSellerId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.approvalStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").value("Failed compliance check: Business registration invalid"));
    }

    @Test
    @DisplayName("Rejecting seller without reason fails with 422 Unprocessable Entity")
    void rejectSeller_missingReasonFails() throws Exception {
        AdminActionRequest request = AdminActionRequest.builder()
                .reason("")
                .build();

        mockMvc.perform(post("/api/v1/admin/sellers/{sellerId}/reject", pendingSellerId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Admin can suspend seller account and storefront")
    void suspendSeller_success() throws Exception {
        // First approve the seller
        mockMvc.perform(post("/api/v1/admin/sellers/{sellerId}/approve", pendingSellerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Suspend
        AdminActionRequest request = AdminActionRequest.builder()
                .reason("Multiple counterfeit item reports")
                .build();

        mockMvc.perform(post("/api/v1/admin/sellers/{sellerId}/suspend", pendingSellerId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.approvalStatus").value("SUSPENDED"));

        // Verify store status is SUSPENDED
        StoreEntity store = storeRepository.findById(pendingSellerStoreId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(StoreStatus.SUSPENDED, store.getStatus());
    }

    @Test
    @DisplayName("Customer role without admin privileges cannot access Admin Seller endpoints -> 403 Forbidden")
    void customerAccess_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sellers")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/sellers/{sellerId}/approve", pendingSellerId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to Admin Seller endpoints returns 401 Unauthorized")
    void unauthenticatedAccess_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sellers"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/admin/sellers/{sellerId}/approve", pendingSellerId))
                .andExpect(status().isUnauthorized());
    }
}
