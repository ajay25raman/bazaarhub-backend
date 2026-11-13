package com.bazaarhub.backend.seller;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.RoleEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataRoleRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.seller.application.dto.request.*;
import com.bazaarhub.backend.seller.application.dto.response.*;
import com.bazaarhub.backend.seller.application.service.*;
import com.bazaarhub.backend.seller.domain.exception.*;
import com.bazaarhub.backend.seller.domain.model.*;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.*;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.*;
import com.bazaarhub.backend.shared.domain.exception.BusinessRuleViolationException;
import com.bazaarhub.backend.shared.infrastructure.security.AesGcmEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerServiceUnitTest {

    @Mock
    private SpringDataSellerRepository sellerRepository;
    @Mock
    private SpringDataUserRepository userRepository;
    @Mock
    private SpringDataRoleRepository roleRepository;
    @Mock
    private SpringDataStoreRepository storeRepository;
    @Mock
    private SpringDataSellerDocumentRepository documentRepository;
    @Mock
    private SpringDataSellerBankAccountRepository bankAccountRepository;

    private AesGcmEncryptionService encryptionService;
    private SellerOnboardingService onboardingService;
    private StoreService storeService;
    private SellerDocumentService documentService;
    private SellerBankService bankService;
    private AdminSellerService adminSellerService;

    private UserEntity user;
    private SellerEntity seller;

    @BeforeEach
    void setUp() {
        encryptionService = new AesGcmEncryptionService("TestEncryptionSecretKeyForAes256Gcm32B!");

        onboardingService = new SellerOnboardingService(
                sellerRepository, userRepository, storeRepository, documentRepository, bankAccountRepository
        );

        storeService = new StoreService(sellerRepository, storeRepository);

        documentService = new SellerDocumentService(sellerRepository, documentRepository);

        bankService = new SellerBankService(sellerRepository, bankAccountRepository, encryptionService);

        adminSellerService = new AdminSellerService(
                sellerRepository, userRepository, roleRepository, documentRepository,
                bankAccountRepository, storeRepository, documentService, bankService
        );

        user = UserEntity.builder()
                .id(100L)
                .fullName("Alice Wonderland")
                .email("alice@example.com")
                .phone("+919123456789")
                .roles(new HashSet<>())
                .build();

        seller = SellerEntity.builder()
                .id(10L)
                .user(user)
                .businessName("Acme Retail")
                .businessEmail("alice@acme.com")
                .businessPhone("+919123456789")
                .taxIdentifier("29ABCDE1234F1Z5")
                .verificationStatus(SellerVerificationStatus.PENDING)
                .approvalStatus(SellerApprovalStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Submit onboarding successfully creates a seller application with PENDING statuses")
    void testSubmitOnboardingSuccess() {
        SellerOnboardingRequest request = SellerOnboardingRequest.builder()
                .businessName("Acme Retail")
                .businessEmail("alice@acme.com")
                .businessPhone("+919123456789")
                .taxIdentifier("29ABCDE1234F1Z5")
                .build();

        when(userRepository.findById(100L)).thenReturn(Optional.of(user));
        when(sellerRepository.existsByUserId(100L)).thenReturn(false);
        when(sellerRepository.save(any(SellerEntity.class))).thenAnswer(invocation -> {
            SellerEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            return entity;
        });

        SellerResponse response = onboardingService.submitOnboarding(100L, request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Acme Retail", response.getBusinessName());
        assertEquals(SellerApprovalStatus.PENDING, response.getApprovalStatus());
        assertEquals(SellerVerificationStatus.PENDING, response.getVerificationStatus());
        verify(sellerRepository).save(any(SellerEntity.class));
    }

    @Test
    @DisplayName("Submit onboarding fails with 409 Conflict if application already exists for user")
    void testSubmitOnboardingDuplicateFails() {
        SellerOnboardingRequest request = SellerOnboardingRequest.builder()
                .businessName("Acme Retail")
                .build();

        when(userRepository.findById(100L)).thenReturn(Optional.of(user));
        when(sellerRepository.existsByUserId(100L)).thenReturn(true);

        assertThrows(SellerAlreadyExistsException.class, () -> onboardingService.submitOnboarding(100L, request));
        verify(sellerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create store succeeds with default INACTIVE status")
    void testCreateStoreSuccess() {
        StoreCreateRequest request = StoreCreateRequest.builder()
                .storeName("Acme Official Store")
                .slug("acme-official-store")
                .description("Official store for Acme")
                .build();

        when(sellerRepository.findByUserId(100L)).thenReturn(Optional.of(seller));
        when(storeRepository.existsBySellerId(10L)).thenReturn(false);
        when(storeRepository.existsBySlug("acme-official-store")).thenReturn(false);
        when(storeRepository.save(any(StoreEntity.class))).thenAnswer(invocation -> {
            StoreEntity store = invocation.getArgument(0);
            store.setId(5L);
            return store;
        });

        StoreResponse response = storeService.createStore(100L, request);

        assertNotNull(response);
        assertEquals("Acme Official Store", response.getStoreName());
        assertEquals("acme-official-store", response.getSlug());
        assertEquals(StoreStatus.INACTIVE, response.getStatus());
        verify(storeRepository).save(any(StoreEntity.class));
    }

    @Test
    @DisplayName("Create store fails if store already exists for seller")
    void testCreateStoreDuplicateFails() {
        StoreCreateRequest request = StoreCreateRequest.builder()
                .storeName("Acme Store")
                .build();

        when(sellerRepository.findByUserId(100L)).thenReturn(Optional.of(seller));
        when(storeRepository.existsBySellerId(10L)).thenReturn(true);

        assertThrows(StoreAlreadyExistsException.class, () -> storeService.createStore(100L, request));
        verify(storeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Document access enforces ownership: accessing another seller's document throws UnauthorizedResourceAccessException")
    void testDocumentOwnershipEnforced() {
        SellerEntity otherSeller = SellerEntity.builder().id(999L).build();
        SellerDocumentEntity doc = SellerDocumentEntity.builder()
                .id(42L)
                .seller(otherSeller)
                .documentType("GST_CERTIFICATE")
                .documentUrl("https://storage.com/kyc.pdf")
                .build();

        when(sellerRepository.findByUserId(100L)).thenReturn(Optional.of(seller)); // seller ID is 10
        when(documentRepository.findById(42L)).thenReturn(Optional.of(doc)); // doc seller ID is 999

        assertThrows(UnauthorizedResourceAccessException.class,
                () -> documentService.getDocumentById(100L, 42L));
    }

    @Test
    @DisplayName("Admin approving seller updates status, assigns ROLE_SELLER, and activates store")
    void testAdminApproveSellerSuccess() {
        UserEntity adminUser = UserEntity.builder().id(1L).fullName("Super Admin").build();
        StoreEntity store = StoreEntity.builder().id(5L).seller(seller).status(StoreStatus.INACTIVE).build();
        seller.setStore(store);

        RoleEntity roleCustomer = RoleEntity.builder().id(1L).name("ROLE_CUSTOMER").build();
        user.getRoles().add(roleCustomer);

        RoleEntity roleSeller = RoleEntity.builder().id(2L).name("ROLE_SELLER").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(sellerRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(seller));
        when(roleRepository.findByName("ROLE_SELLER")).thenReturn(Optional.of(roleSeller));
        when(sellerRepository.save(any(SellerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerResponse response = adminSellerService.approveSeller(1L, 10L);

        assertNotNull(response);
        assertEquals(SellerApprovalStatus.APPROVED, response.getApprovalStatus());
        assertNotNull(seller.getApprovedAt());
        assertEquals(adminUser, seller.getApprovedBy());

        // Verify ROLE_SELLER added and customer role preserved
        assertTrue(user.getRoles().contains(roleSeller));
        assertTrue(user.getRoles().contains(roleCustomer));

        // Verify store activated
        assertEquals(StoreStatus.ACTIVE, store.getStatus());
        verify(userRepository).save(user);
        verify(storeRepository).save(store);
    }

    @Test
    @DisplayName("Admin cannot approve their own seller application")
    void testAdminSelfApprovalRejected() {
        UserEntity adminUser = UserEntity.builder().id(100L).fullName("Alice Admin").build();
        seller.setUser(adminUser); // Admin user owns this application

        when(userRepository.findById(100L)).thenReturn(Optional.of(adminUser));
        when(sellerRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(seller));

        assertThrows(InvalidSellerStateException.class, () -> adminSellerService.approveSeller(100L, 10L));
        verify(sellerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admin rejecting seller requires reason and updates status to REJECTED")
    void testAdminRejectSeller() {
        UserEntity adminUser = UserEntity.builder().id(1L).build();
        AdminActionRequest request = AdminActionRequest.builder().reason("Incomplete tax documents").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(sellerRepository.findById(10L)).thenReturn(Optional.of(seller));
        when(sellerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SellerResponse response = adminSellerService.rejectSeller(1L, 10L, request);

        assertEquals(SellerApprovalStatus.REJECTED, response.getApprovalStatus());
        assertEquals("Incomplete tax documents", seller.getRejectionReason());
    }

    @Test
    @DisplayName("Admin rejecting seller without reason throws BusinessRuleViolationException")
    void testAdminRejectSellerWithoutReasonFails() {
        UserEntity adminUser = UserEntity.builder().id(1L).build();
        AdminActionRequest request = AdminActionRequest.builder().reason("   ").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(sellerRepository.findById(10L)).thenReturn(Optional.of(seller));

        assertThrows(BusinessRuleViolationException.class,
                () -> adminSellerService.rejectSeller(1L, 10L, request));
    }

    @Test
    @DisplayName("Admin suspending seller updates status to SUSPENDED and halts storefront")
    void testAdminSuspendSeller() {
        UserEntity adminUser = UserEntity.builder().id(1L).build();
        StoreEntity store = StoreEntity.builder().id(5L).seller(seller).status(StoreStatus.ACTIVE).build();
        seller.setStore(store);

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(sellerRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(seller));
        when(sellerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SellerResponse response = adminSellerService.suspendSeller(1L, 10L, AdminActionRequest.builder().reason("Compliance audit pending").build());

        assertEquals(SellerApprovalStatus.SUSPENDED, response.getApprovalStatus());
        assertEquals(StoreStatus.SUSPENDED, store.getStatus());
        verify(storeRepository).save(store);
    }
}
