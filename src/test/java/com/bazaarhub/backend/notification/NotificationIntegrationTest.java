package com.bazaarhub.backend.notification;

import com.bazaarhub.backend.identity.application.dto.LoginRequest;
import com.bazaarhub.backend.identity.application.dto.RegisterRequest;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.RoleEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataRoleRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.notification.application.dto.request.CreateNotificationTemplateRequest;
import com.bazaarhub.backend.notification.application.dto.request.NotificationPreferenceRequest;
import com.bazaarhub.backend.notification.application.dto.request.SendNotificationRequest;
import com.bazaarhub.backend.notification.application.dto.request.UpdateNotificationTemplateRequest;
import com.bazaarhub.backend.notification.domain.model.DeliveryStatus;
import com.bazaarhub.backend.notification.domain.model.NotificationChannel;
import com.bazaarhub.backend.notification.domain.model.NotificationType;
import com.bazaarhub.backend.notification.domain.model.TemplateStatus;
import com.bazaarhub.backend.notification.infrastructure.persistence.entity.NotificationEntity;
import com.bazaarhub.backend.notification.infrastructure.persistence.entity.NotificationTemplateEntity;
import com.bazaarhub.backend.notification.infrastructure.persistence.repository.SpringDataNotificationRepository;
import com.bazaarhub.backend.notification.infrastructure.persistence.repository.SpringDataNotificationTemplateRepository;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.domain.model.StoreStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.StoreEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataStoreRepository;
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

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationIntegrationTest {

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

    @Autowired
    private SpringDataStoreRepository storeRepository;

    @Autowired
    private SpringDataNotificationRepository notificationRepository;

    @Autowired
    private SpringDataNotificationTemplateRepository templateRepository;

    private String customerToken;
    private String secondCustomerToken;
    private String sellerToken;
    private String adminToken;

    private UserEntity customerUser;
    private UserEntity secondCustomerUser;
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

        // 1. Customer 1
        customerToken = registerAndLogin("notif.customer1@bazaarhub.com", "Password@123", "Customer One", "+919876542001");
        customerUser = userRepository.findByEmail("notif.customer1@bazaarhub.com").orElseThrow();

        // 2. Customer 2
        secondCustomerToken = registerAndLogin("notif.customer2@bazaarhub.com", "Password@123", "Customer Two", "+919876542002");
        secondCustomerUser = userRepository.findByEmail("notif.customer2@bazaarhub.com").orElseThrow();

        // 3. Seller
        registerAndLogin("notif.seller@bazaarhub.com", "Password@123", "Merchant Suresh", "+919876542003");
        sellerUser = userRepository.findByEmail("notif.seller@bazaarhub.com").orElseThrow();
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", sellerUser.getId(), sellerRole.getId());
        sellerToken = login("notif.seller@bazaarhub.com", "Password@123");

        SellerEntity seller = sellerRepository.save(SellerEntity.builder()
                .user(sellerUser)
                .businessName("Bazaar Smart Audio Store")
                .businessEmail("notif.seller@bazaarhub.com")
                .businessPhone("+919876542003")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .build());

        storeRepository.save(StoreEntity.builder()
                .seller(seller)
                .storeName("Bazaar Smart Audio Store")
                .slug("bazaar-notif-store-" + UUID.randomUUID().toString().substring(0, 8))
                .status(StoreStatus.ACTIVE)
                .build());

        // 4. Admin
        registerAndLogin("notif.admin@bazaarhub.com", "Password@123", "Admin Officer", "+919876542004");
        adminUser = userRepository.findByEmail("notif.admin@bazaarhub.com").orElseThrow();
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", adminUser.getId(), adminRole.getId());
        adminToken = login("notif.admin@bazaarhub.com", "Password@123");
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("DELETE FROM notifications");
        jdbcTemplate.execute("DELETE FROM notification_templates");
        jdbcTemplate.execute("DELETE FROM stores");
        jdbcTemplate.execute("DELETE FROM sellers");
        jdbcTemplate.execute("DELETE FROM user_devices");
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
    @DisplayName("Customer Notification API: List, unread count, mark as read, mark all read, preferences")
    void testCustomerNotificationLifecycle() throws Exception {
        // 1. Initial notifications empty
        mockMvc.perform(get("/api/v1/customer/notifications")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(0));

        // 2. Initial unread count 0
        mockMvc.perform(get("/api/v1/customer/notifications/unread-count")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));

        // 3. Seed two in-app notifications for customer 1
        NotificationEntity n1 = notificationRepository.save(NotificationEntity.builder()
                .user(customerUser)
                .channel(NotificationChannel.IN_APP)
                .notificationType(NotificationType.ORDER_PLACED.name())
                .title("Order Confirmed")
                .message("Your order has been placed successfully.")
                .deliveryStatus(DeliveryStatus.SENT)
                .read(false)
                .build());

        NotificationEntity n2 = notificationRepository.save(NotificationEntity.builder()
                .user(customerUser)
                .channel(NotificationChannel.IN_APP)
                .notificationType(NotificationType.ORDER_SHIPPED.name())
                .title("Order Shipped")
                .message("Your order is on the way.")
                .deliveryStatus(DeliveryStatus.SENT)
                .read(false)
                .build());

        // 4. Verify unread count becomes 2
        mockMvc.perform(get("/api/v1/customer/notifications/unread-count")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(2));

        // 5. Mark n1 as read
        mockMvc.perform(patch("/api/v1/customer/notifications/" + n1.getId() + "/read")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true))
                .andExpect(jsonPath("$.data.deliveryStatus").value("READ"))
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());

        // 6. Verify unread count becomes 1
        mockMvc.perform(get("/api/v1/customer/notifications/unread-count")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        // 7. Security check: Customer 2 attempts to mark n2 (owned by customer 1) as read -> 403 FORBIDDEN
        mockMvc.perform(patch("/api/v1/customer/notifications/" + n2.getId() + "/read")
                        .header("Authorization", "Bearer " + secondCustomerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        // 8. Mark all remaining notifications as read
        mockMvc.perform(patch("/api/v1/customer/notifications/read-all")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        // 9. Unread count is now 0
        mockMvc.perform(get("/api/v1/customer/notifications/unread-count")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));

        // 10. Check preferences GET
        mockMvc.perform(get("/api/v1/customer/notification-preferences")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inAppEnabled").value(true))
                .andExpect(jsonPath("$.data.pushEnabled").value(true))
                .andExpect(jsonPath("$.data.emailEnabled").value(true));

        // 11. Update preferences PUT
        NotificationPreferenceRequest prefRequest = NotificationPreferenceRequest.builder()
                .inAppEnabled(true)
                .pushEnabled(false)
                .emailEnabled(true)
                .smsEnabled(false)
                .build();

        mockMvc.perform(put("/api/v1/customer/notification-preferences")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prefRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pushEnabled").value(false));
    }

    @Test
    @DisplayName("Seller Notification API: Seller receives sub-order notifications and marks them read")
    void testSellerNotificationLifecycle() throws Exception {
        // Seed seller notification
        NotificationEntity sellerNotif = notificationRepository.save(NotificationEntity.builder()
                .user(sellerUser)
                .channel(NotificationChannel.IN_APP)
                .notificationType(NotificationType.DELIVERY_ASSIGNED.name())
                .title("New Order Received")
                .message("You have received a new customer sub-order to fulfill.")
                .deliveryStatus(DeliveryStatus.SENT)
                .read(false)
                .build());

        // 1. Seller queries notifications
        mockMvc.perform(get("/api/v1/seller/notifications")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("New Order Received"));

        // 2. Seller checks unread count
        mockMvc.perform(get("/api/v1/seller/notifications/unread-count")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        // 3. Seller marks read
        mockMvc.perform(patch("/api/v1/seller/notifications/" + sellerNotif.getId() + "/read")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true));

        // 4. Seller preferences
        mockMvc.perform(get("/api/v1/seller/notification-preferences")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inAppEnabled").value(true));
    }

    @Test
    @DisplayName("Admin Notification and Template API: Send notification, test deduplication, template CRUD")
    void testAdminNotificationAndTemplateManagement() throws Exception {
        // 1. Admin dispatches notification to customer
        SendNotificationRequest sendRequest = SendNotificationRequest.builder()
                .userId(customerUser.getId())
                .channel(NotificationChannel.IN_APP)
                .notificationType(NotificationType.SYSTEM_ALERT)
                .title("Welcome Promotion")
                .message("Enjoy 10% off on your first order!")
                .deduplicationKey("PROMO-USER-" + customerUser.getId())
                .build();

        MvcResult sendResult = mockMvc.perform(post("/api/v1/admin/notifications/send")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Welcome Promotion"))
                .andReturn();

        JsonNode sendNode = objectMapper.readTree(sendResult.getResponse().getContentAsString());
        Long createdNotifId = sendNode.path("data").path("id").asLong();

        // 2. Test Deduplication: sending identical request returns same entity ID without duplicate
        mockMvc.perform(post("/api/v1/admin/notifications/send")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(createdNotifId));

        // 3. Admin queries all system notifications with filters
        mockMvc.perform(get("/api/v1/admin/notifications")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("channel", "IN_APP")
                        .param("userId", customerUser.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        // 4. Create Notification Template
        CreateNotificationTemplateRequest templateRequest = CreateNotificationTemplateRequest.builder()
                .templateKey("WELCOME_EMAIL_V1")
                .channel(NotificationChannel.EMAIL)
                .subject("Welcome to BazaarHub, {{firstName}}!")
                .bodyTemplate("Dear {{firstName}}, we are thrilled to welcome you.")
                .status(TemplateStatus.ACTIVE)
                .build();

        MvcResult templateResult = mockMvc.perform(post("/api/v1/admin/notifications/templates")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(templateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.templateKey").value("WELCOME_EMAIL_V1"))
                .andExpect(jsonPath("$.data.channel").value("EMAIL"))
                .andReturn();

        JsonNode templateNode = objectMapper.readTree(templateResult.getResponse().getContentAsString());
        Long templateId = templateNode.path("data").path("id").asLong();

        // 5. Get template by ID
        mockMvc.perform(get("/api/v1/admin/notifications/templates/" + templateId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subject").value("Welcome to BazaarHub, {{firstName}}!"));

        // 6. Update template
        UpdateNotificationTemplateRequest updateRequest = UpdateNotificationTemplateRequest.builder()
                .subject("Welcome to BazaarHub India, {{firstName}}!")
                .bodyTemplate("Updated body text")
                .status(TemplateStatus.ACTIVE)
                .build();

        mockMvc.perform(put("/api/v1/admin/notifications/templates/" + templateId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subject").value("Welcome to BazaarHub India, {{firstName}}!"));

        // 7. Security: Customer cannot access admin notifications
        mockMvc.perform(get("/api/v1/admin/notifications")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }
}
