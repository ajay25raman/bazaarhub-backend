package com.bazaarhub.backend.support;

import com.bazaarhub.backend.identity.application.dto.LoginRequest;
import com.bazaarhub.backend.identity.application.dto.RegisterRequest;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.RoleEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataRoleRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.support.application.dto.request.*;
import com.bazaarhub.backend.support.domain.model.TicketCategory;
import com.bazaarhub.backend.support.domain.model.TicketPriority;
import com.bazaarhub.backend.support.domain.model.TicketStatus;
import com.bazaarhub.backend.support.infrastructure.persistence.repository.SpringDataSupportMessageRepository;
import com.bazaarhub.backend.support.infrastructure.persistence.repository.SpringDataSupportTicketRepository;
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

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SupportTicketIntegrationTest {

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
    private SpringDataSupportTicketRepository ticketRepository;

    @Autowired
    private SpringDataSupportMessageRepository messageRepository;

    private String customerToken;
    private String secondCustomerToken;
    private String sellerToken;
    private String deliveryToken;
    private String adminToken;

    private UserEntity customerUser;
    private UserEntity secondCustomerUser;
    private UserEntity sellerUser;
    private UserEntity deliveryUser;
    private UserEntity adminUser;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();

        RoleEntity customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_CUSTOMER").description("Customer").build()));
        RoleEntity sellerRole = roleRepository.findByName("ROLE_SELLER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_SELLER").description("Seller").build()));
        RoleEntity deliveryRole = roleRepository.findByName("ROLE_DELIVERY")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_DELIVERY").description("Delivery").build()));
        RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_ADMIN").description("Admin").build()));

        // 1. Customer 1
        customerToken = registerAndLogin("supp.customer1@bazaarhub.com", "Password@123", "Customer One", "+919876550001");
        customerUser = userRepository.findByEmail("supp.customer1@bazaarhub.com").orElseThrow();

        // 2. Customer 2
        secondCustomerToken = registerAndLogin("supp.customer2@bazaarhub.com", "Password@123", "Customer Two", "+919876550002");
        secondCustomerUser = userRepository.findByEmail("supp.customer2@bazaarhub.com").orElseThrow();

        // 3. Seller
        registerAndLogin("supp.seller@bazaarhub.com", "Password@123", "Merchant Suresh", "+919876550003");
        sellerUser = userRepository.findByEmail("supp.seller@bazaarhub.com").orElseThrow();
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", sellerUser.getId(), sellerRole.getId());
        sellerToken = login("supp.seller@bazaarhub.com", "Password@123");

        // 4. Delivery Personnel
        registerAndLogin("supp.delivery@bazaarhub.com", "Password@123", "Courier Ramesh", "+919876550004");
        deliveryUser = userRepository.findByEmail("supp.delivery@bazaarhub.com").orElseThrow();
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", deliveryUser.getId(), deliveryRole.getId());
        deliveryToken = login("supp.delivery@bazaarhub.com", "Password@123");

        // 5. Admin
        registerAndLogin("supp.admin@bazaarhub.com", "Password@123", "Admin Officer", "+919876550005");
        adminUser = userRepository.findByEmail("supp.admin@bazaarhub.com").orElseThrow();
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", adminUser.getId(), adminRole.getId());
        adminToken = login("supp.admin@bazaarhub.com", "Password@123");
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("DELETE FROM support_messages");
        jdbcTemplate.execute("DELETE FROM support_tickets");
        jdbcTemplate.execute("DELETE FROM notifications");
        jdbcTemplate.execute("DELETE FROM notification_templates");
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
    @DisplayName("Customer Support Lifecycle: Submit ticket, reply, view conversation, close ticket, upload attachment")
    void testCustomerSupportLifecycle() throws Exception {
        // 1. Submit ticket
        CreateTicketRequest createRequest = CreateTicketRequest.builder()
                .category(TicketCategory.ORDER_ISSUE)
                .subject("Damaged item received")
                .description("The outer box was opened and headphones cracked.")
                .priority(TicketPriority.HIGH)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/customer/tickets")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.category").value("ORDER_ISSUE"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.ticketNumber").value(startsWith("TKT-")))
                .andReturn();

        Long ticketId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. List customer tickets
        mockMvc.perform(get("/api/v1/customer/tickets")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(ticketId));

        // 3. Customer posts reply
        AddTicketMessageRequest messageRequest = AddTicketMessageRequest.builder()
                .message("Attaching photo of the cracked headset.")
                .attachmentUrl("https://res.cloudinary.com/demo/image/upload/v1/damaged.jpg")
                .build();

        mockMvc.perform(post("/api/v1/customer/tickets/" + ticketId + "/messages")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messageRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.message").value("Attaching photo of the cracked headset."))
                .andExpect(jsonPath("$.data.attachmentUrl").value("https://res.cloudinary.com/demo/image/upload/v1/damaged.jpg"));

        // 4. View messages
        mockMvc.perform(get("/api/v1/customer/tickets/" + ticketId + "/messages")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        // 5. Security: Customer 2 cannot access Customer 1's ticket -> 403 Forbidden
        mockMvc.perform(get("/api/v1/customer/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + secondCustomerToken))
                .andExpect(status().isForbidden());

        // 6. Security: Customer 2 cannot post message to Customer 1's ticket -> 403 Forbidden
        mockMvc.perform(post("/api/v1/customer/tickets/" + ticketId + "/messages")
                        .header("Authorization", "Bearer " + secondCustomerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messageRequest)))
                .andExpect(status().isForbidden());

        // 7. Upload attachment endpoint
        mockMvc.perform(post("/api/v1/customer/tickets/attachments")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("image", "https://example.com/photo.png"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attachmentUrl").value("https://example.com/photo.png"));

        // 8. Customer closes own ticket
        mockMvc.perform(patch("/api/v1/customer/tickets/" + ticketId + "/close")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.closedAt").isNotEmpty());
    }

    @Test
    @DisplayName("Seller & Delivery Support APIs: Merchant and courier create and view tickets")
    void testSellerAndDeliverySupport() throws Exception {
        // 1. Seller creates ticket
        CreateTicketRequest sellerTicket = CreateTicketRequest.builder()
                .category(TicketCategory.SELLER_ISSUE)
                .subject("Payout reconciliation discrepancy")
                .description("Weekly settlement is missing commission credit for SO-1002.")
                .priority(TicketPriority.MEDIUM)
                .build();

        MvcResult sellerResult = mockMvc.perform(post("/api/v1/seller/tickets")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sellerTicket)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.category").value("SELLER_ISSUE"))
                .andReturn();

        Long sellerTicketId = objectMapper.readTree(sellerResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. Seller lists tickets
        mockMvc.perform(get("/api/v1/seller/tickets")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        // 3. Delivery partner creates ticket
        CreateTicketRequest deliveryTicket = CreateTicketRequest.builder()
                .category(TicketCategory.DELIVERY_ISSUE)
                .subject("Customer address locked building")
                .description("Customer is unreachable by phone and building security does not allow entry.")
                .priority(TicketPriority.URGENT)
                .build();

        mockMvc.perform(post("/api/v1/delivery/tickets")
                        .header("Authorization", "Bearer " + deliveryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deliveryTicket)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.category").value("DELIVERY_ISSUE"))
                .andExpect(jsonPath("$.data.priority").value("URGENT"));
    }

    @Test
    @DisplayName("Admin Support Desk: Triage, assignment, internal notes, resolution, and stats")
    void testAdminSupportDeskOperations() throws Exception {
        // Customer creates ticket
        CreateTicketRequest createRequest = CreateTicketRequest.builder()
                .category(TicketCategory.PAYMENT_ISSUE)
                .subject("Double payment charged")
                .description("Charged twice on Razorpay")
                .priority(TicketPriority.HIGH)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/customer/tickets")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long ticketId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 1. Admin queries tickets with category and status filter
        mockMvc.perform(get("/api/v1/admin/support/tickets")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("category", "PAYMENT_ISSUE")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        // 2. Admin assigns ticket to admin user (acts as agent)
        AssignTicketRequest assignRequest = AssignTicketRequest.builder()
                .assignedToUserId(adminUser.getId())
                .build();

        mockMvc.perform(patch("/api/v1/admin/support/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.assignedToId").value(adminUser.getId()));

        // 3. Admin posts PRIVATE INTERNAL NOTE (isInternal = true)
        AddTicketMessageRequest internalNote = AddTicketMessageRequest.builder()
                .message("Internal note: Checking with Razorpay merchant portal for transaction ARN.")
                .isInternal(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/support/tickets/" + ticketId + "/messages")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(internalNote)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isInternal").value(true));

        // 4. Admin posts PUBLIC AGENT RESPONSE
        AddTicketMessageRequest publicReply = AddTicketMessageRequest.builder()
                .message("We have identified the duplicate debit and initiated a refund to your bank.")
                .isInternal(false)
                .build();

        mockMvc.perform(post("/api/v1/admin/support/tickets/" + ticketId + "/messages")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(publicReply)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isInternal").value(false));

        // 5. Customer views messages: Internal note MUST BE HIDDEN (only 1 public message returned)
        mockMvc.perform(get("/api/v1/customer/tickets/" + ticketId + "/messages")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].isInternal").value(false))
                .andExpect(jsonPath("$.data.content[0].message").value(containsString("identified the duplicate debit")));

        // 6. Admin views messages: Internal note MUST BE VISIBLE (both 2 messages returned)
        mockMvc.perform(get("/api/v1/admin/support/tickets/" + ticketId + "/messages")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));

        // 7. Admin updates priority to URGENT
        UpdateTicketPriorityRequest priorityRequest = UpdateTicketPriorityRequest.builder()
                .priority(TicketPriority.URGENT)
                .build();

        mockMvc.perform(patch("/api/v1/admin/support/tickets/" + ticketId + "/priority")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(priorityRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.priority").value("URGENT"));

        // 8. Admin resolves ticket
        UpdateTicketStatusRequest resolveRequest = UpdateTicketStatusRequest.builder()
                .status(TicketStatus.RESOLVED)
                .resolutionNotes("Refund TXN #88721 completed.")
                .build();

        mockMvc.perform(patch("/api/v1/admin/support/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolvedAt").isNotEmpty());

        // 9. Admin views ticket stats
        mockMvc.perform(get("/api/v1/admin/support/tickets/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resolvedCount").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalCount").value(greaterThanOrEqualTo(1)));

        // 10. Security: Customer cannot access admin endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/v1/admin/support/tickets")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }
}
