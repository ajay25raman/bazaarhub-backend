package com.bazaarhub.backend.support;

import com.bazaarhub.backend.delivery.infrastructure.adapter.CloudinaryService;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.notification.application.service.NotificationService;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.bazaarhub.backend.support.application.dto.request.AddTicketMessageRequest;
import com.bazaarhub.backend.support.application.dto.request.CreateTicketRequest;
import com.bazaarhub.backend.support.application.dto.request.UpdateTicketPriorityRequest;
import com.bazaarhub.backend.support.application.dto.request.UpdateTicketStatusRequest;
import com.bazaarhub.backend.support.application.dto.response.SupportMessageResponse;
import com.bazaarhub.backend.support.application.dto.response.SupportTicketResponse;
import com.bazaarhub.backend.support.application.dto.response.TicketStatsResponse;
import com.bazaarhub.backend.support.application.mapper.SupportMessageMapper;
import com.bazaarhub.backend.support.application.mapper.SupportTicketMapper;
import com.bazaarhub.backend.support.application.service.SupportTicketServiceImpl;
import com.bazaarhub.backend.support.domain.exception.InvalidTicketOperationException;
import com.bazaarhub.backend.support.domain.exception.SupportTicketNotFoundException;
import com.bazaarhub.backend.support.domain.exception.UnauthorizedTicketAccessException;
import com.bazaarhub.backend.support.domain.model.TicketCategory;
import com.bazaarhub.backend.support.domain.model.TicketPriority;
import com.bazaarhub.backend.support.domain.model.TicketStatus;
import com.bazaarhub.backend.support.infrastructure.persistence.entity.SupportMessageEntity;
import com.bazaarhub.backend.support.infrastructure.persistence.entity.SupportTicketEntity;
import com.bazaarhub.backend.support.infrastructure.persistence.repository.SpringDataSupportMessageRepository;
import com.bazaarhub.backend.support.infrastructure.persistence.repository.SpringDataSupportTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportTicketServiceUnitTest {

    @Mock
    private SpringDataSupportTicketRepository ticketRepository;

    @Mock
    private SpringDataSupportMessageRepository messageRepository;

    @Mock
    private SpringDataUserRepository userRepository;

    @Mock
    private SpringDataOrderRepository orderRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private NotificationService notificationService;

    @Spy
    private SupportTicketMapper ticketMapper = new SupportTicketMapper();

    @Spy
    private SupportMessageMapper messageMapper = new SupportMessageMapper();

    @InjectMocks
    private SupportTicketServiceImpl ticketService;

    private UserEntity customer;
    private UserEntity otherCustomer;
    private UserEntity agent;
    private UserEntity admin;
    private OrderEntity order;

    @BeforeEach
    void setUp() {
        customer = UserEntity.builder()
                .id(100L)
                .email("customer@example.com")
                .fullName("Customer Ajay")
                .phone("+919876541001")
                .build();

        otherCustomer = UserEntity.builder()
                .id(200L)
                .email("other@example.com")
                .fullName("Customer Bob")
                .phone("+919876541002")
                .build();

        agent = UserEntity.builder()
                .id(300L)
                .email("agent@bazaarhub.com")
                .fullName("Support Agent Priya")
                .phone("+919876541003")
                .build();

        admin = UserEntity.builder()
                .id(400L)
                .email("admin@bazaarhub.com")
                .fullName("Admin Officer")
                .phone("+919876541004")
                .build();

        order = OrderEntity.builder()
                .id(50L)
                .orderNumber("ORD-20261115-TEST")
                .user(customer)
                .build();
    }

    @Test
    @DisplayName("Create Ticket: Success creates ticket in OPEN status and dispatches notification")
    void testCreateTicket_Success() {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .category(TicketCategory.PAYMENT_ISSUE)
                .subject("Payment debited twice")
                .description("Amount was deducted twice on UPI")
                .priority(TicketPriority.HIGH)
                .orderId(50L)
                .build();

        when(userRepository.findById(100L)).thenReturn(Optional.of(customer));
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        when(ticketRepository.save(any(SupportTicketEntity.class))).thenAnswer(invocation -> {
            SupportTicketEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            entity.setCreatedAt(Instant.now());
            entity.setUpdatedAt(Instant.now());
            return entity;
        });

        when(messageRepository.countByTicketId(1L)).thenReturn(0L);

        SupportTicketResponse response = ticketService.createTicket(100L, request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Payment debited twice", response.getSubject());
        assertEquals(TicketCategory.PAYMENT_ISSUE, response.getCategory());
        assertEquals(TicketPriority.HIGH, response.getPriority());
        assertEquals(TicketStatus.OPEN, response.getStatus());
        assertNotNull(response.getTicketNumber());
        assertTrue(response.getTicketNumber().startsWith("TKT-"));

        verify(notificationService).sendNotification(any());
    }

    @Test
    @DisplayName("Create Ticket: Creates initial message with attachment if attachmentUrl provided")
    void testCreateTicket_WithAttachment() {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .category(TicketCategory.PRODUCT_ISSUE)
                .subject("Damaged item")
                .description("The package was opened")
                .attachmentUrl("https://res.cloudinary.com/demo/image/upload/proof.jpg")
                .build();

        when(userRepository.findById(100L)).thenReturn(Optional.of(customer));
        when(ticketRepository.save(any(SupportTicketEntity.class))).thenAnswer(invocation -> {
            SupportTicketEntity entity = invocation.getArgument(0);
            entity.setId(2L);
            entity.setCreatedAt(Instant.now());
            return entity;
        });

        ticketService.createTicket(100L, request);

        verify(messageRepository).save(argThat(msg ->
                msg.getAttachmentUrl().equals("https://res.cloudinary.com/demo/image/upload/proof.jpg") &&
                msg.getTicket().getId().equals(2L)
        ));
    }

    @Test
    @DisplayName("Get Ticket Details: Success for ticket owner")
    void testGetTicketDetails_Owner_Success() {
        SupportTicketEntity ticket = SupportTicketEntity.builder()
                .id(10L)
                .ticketNumber("TKT-20261115-AAAA")
                .user(customer)
                .category(TicketCategory.DELIVERY_ISSUE)
                .subject("Late package")
                .description("Not delivered yet")
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.MEDIUM)
                .build();

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(messageRepository.countByTicketIdAndInternalFalse(10L)).thenReturn(2L);

        SupportTicketResponse response = ticketService.getTicketDetails(10L, 100L, false);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(2L, response.getMessageCount());
    }

    @Test
    @DisplayName("Get Ticket Details: Throws UnauthorizedTicketAccessException when non-owner customer accesses")
    void testGetTicketDetails_Unauthorized_ThrowsException() {
        SupportTicketEntity ticket = SupportTicketEntity.builder()
                .id(10L)
                .ticketNumber("TKT-20261115-AAAA")
                .user(customer) // Owned by 100
                .build();

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));

        assertThrows(UnauthorizedTicketAccessException.class, () ->
                ticketService.getTicketDetails(10L, 200L, false)); // Requested by 200
    }

    @Test
    @DisplayName("Assign Ticket: Assigns agent and updates status to IN_PROGRESS")
    void testAssignTicket_Success() {
        SupportTicketEntity ticket = SupportTicketEntity.builder()
                .id(10L)
                .ticketNumber("TKT-20261115-AAAA")
                .user(customer)
                .status(TicketStatus.OPEN)
                .build();

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(300L)).thenReturn(Optional.of(agent));
        when(userRepository.findById(400L)).thenReturn(Optional.of(admin));
        when(ticketRepository.save(any(SupportTicketEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupportTicketResponse response = ticketService.assignTicket(10L, 300L, 400L);

        assertNotNull(response);
        assertEquals(TicketStatus.IN_PROGRESS, response.getStatus());
        assertEquals(300L, response.getAssignedToId());
        assertEquals("Support Agent Priya", response.getAssignedToName());

        verify(notificationService, times(2)).sendNotification(any());
    }

    @Test
    @DisplayName("Update Status: Staff resolves ticket and sets resolvedAt")
    void testUpdateStatus_StaffResolve() {
        SupportTicketEntity ticket = SupportTicketEntity.builder()
                .id(10L)
                .ticketNumber("TKT-20261115-AAAA")
                .user(customer)
                .status(TicketStatus.IN_PROGRESS)
                .build();

        UpdateTicketStatusRequest request = UpdateTicketStatusRequest.builder()
                .status(TicketStatus.RESOLVED)
                .resolutionNotes("Resolution confirmed with bank")
                .build();

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(400L)).thenReturn(Optional.of(admin));
        when(ticketRepository.save(any(SupportTicketEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupportTicketResponse response = ticketService.updateStatus(10L, request, 400L, true);

        assertNotNull(response);
        assertEquals(TicketStatus.RESOLVED, response.getStatus());
        assertNotNull(response.getResolvedAt());
        verify(messageRepository).save(any(SupportMessageEntity.class));
        verify(notificationService).sendNotification(any());
    }

    @Test
    @DisplayName("Update Status: Customer cannot set status other than CLOSED")
    void testUpdateStatus_CustomerInvalidStatus() {
        SupportTicketEntity ticket = SupportTicketEntity.builder()
                .id(10L)
                .user(customer)
                .status(TicketStatus.IN_PROGRESS)
                .build();

        UpdateTicketStatusRequest request = UpdateTicketStatusRequest.builder()
                .status(TicketStatus.RESOLVED) // Customer attempting to self-resolve
                .build();

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(100L)).thenReturn(Optional.of(customer));

        assertThrows(InvalidTicketOperationException.class, () ->
                ticketService.updateStatus(10L, request, 100L, false));
    }

    @Test
    @DisplayName("Add Message: Customer reply transitions ticket from WAITING_FOR_CUSTOMER to IN_PROGRESS")
    void testAddMessage_CustomerReply_TransitionsState() {
        SupportTicketEntity ticket = SupportTicketEntity.builder()
                .id(10L)
                .ticketNumber("TKT-20261115-AAAA")
                .user(customer)
                .assignedTo(agent)
                .status(TicketStatus.WAITING_FOR_CUSTOMER)
                .build();

        AddTicketMessageRequest request = AddTicketMessageRequest.builder()
                .message("Here is the requested bank transaction screenshot.")
                .build();

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(100L)).thenReturn(Optional.of(customer));
        when(messageRepository.save(any(SupportMessageEntity.class))).thenAnswer(invocation -> {
            SupportMessageEntity entity = invocation.getArgument(0);
            entity.setId(5L);
            return entity;
        });

        SupportMessageResponse messageResponse = ticketService.addMessage(10L, request, 100L, false);

        assertNotNull(messageResponse);
        assertEquals("Here is the requested bank transaction screenshot.", messageResponse.getMessage());
        assertFalse(messageResponse.isInternal());
        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
        verify(ticketRepository).save(ticket);
    }

    @Test
    @DisplayName("Add Message: Staff internal note is marked internal and does not notify customer")
    void testAddMessage_StaffInternalNote() {
        SupportTicketEntity ticket = SupportTicketEntity.builder()
                .id(10L)
                .ticketNumber("TKT-20261115-AAAA")
                .user(customer)
                .status(TicketStatus.IN_PROGRESS)
                .build();

        AddTicketMessageRequest request = AddTicketMessageRequest.builder()
                .message("Internal note: Customer contacted bank, waiting for payment trace ID.")
                .isInternal(true)
                .build();

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(300L)).thenReturn(Optional.of(agent));
        when(messageRepository.save(any(SupportMessageEntity.class))).thenAnswer(invocation -> {
            SupportMessageEntity entity = invocation.getArgument(0);
            entity.setId(6L);
            return entity;
        });

        SupportMessageResponse messageResponse = ticketService.addMessage(10L, request, 300L, true);

        assertNotNull(messageResponse);
        assertTrue(messageResponse.isInternal());
        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    @DisplayName("Add Message: Throws exception when ticket is CLOSED")
    void testAddMessage_ClosedTicket_ThrowsException() {
        SupportTicketEntity ticket = SupportTicketEntity.builder()
                .id(10L)
                .user(customer)
                .status(TicketStatus.CLOSED)
                .build();

        AddTicketMessageRequest request = AddTicketMessageRequest.builder()
                .message("Reply after ticket is closed")
                .build();

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(100L)).thenReturn(Optional.of(customer));

        assertThrows(InvalidTicketOperationException.class, () ->
                ticketService.addMessage(10L, request, 100L, false));
    }

    @Test
    @DisplayName("Ticket Stats: Returns accurate counts across lifecycle states")
    void testGetTicketStats_Success() {
        when(ticketRepository.countByStatus(TicketStatus.OPEN)).thenReturn(5L);
        when(ticketRepository.countByStatus(TicketStatus.IN_PROGRESS)).thenReturn(3L);
        when(ticketRepository.countByStatus(TicketStatus.WAITING_FOR_CUSTOMER)).thenReturn(2L);
        when(ticketRepository.countByStatus(TicketStatus.RESOLVED)).thenReturn(10L);
        when(ticketRepository.countByStatus(TicketStatus.CLOSED)).thenReturn(20L);

        TicketStatsResponse stats = ticketService.getTicketStats();

        assertNotNull(stats);
        assertEquals(5L, stats.getOpenCount());
        assertEquals(3L, stats.getInProgressCount());
        assertEquals(2L, stats.getWaitingForCustomerCount());
        assertEquals(10L, stats.getResolvedCount());
        assertEquals(20L, stats.getClosedCount());
        assertEquals(40L, stats.getTotalCount());
    }
}
