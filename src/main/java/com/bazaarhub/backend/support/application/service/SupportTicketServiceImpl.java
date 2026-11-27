package com.bazaarhub.backend.support.application.service;

import com.bazaarhub.backend.delivery.infrastructure.adapter.CloudinaryService;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.notification.application.dto.request.SendNotificationRequest;
import com.bazaarhub.backend.notification.application.service.NotificationService;
import com.bazaarhub.backend.notification.domain.model.NotificationChannel;
import com.bazaarhub.backend.notification.domain.model.NotificationType;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;
import com.bazaarhub.backend.support.application.dto.request.*;
import com.bazaarhub.backend.support.application.dto.response.SupportMessageResponse;
import com.bazaarhub.backend.support.application.dto.response.SupportTicketResponse;
import com.bazaarhub.backend.support.application.dto.response.TicketStatsResponse;
import com.bazaarhub.backend.support.application.mapper.SupportMessageMapper;
import com.bazaarhub.backend.support.application.mapper.SupportTicketMapper;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Transactional
public class SupportTicketServiceImpl implements SupportTicketService {

    private static final Logger log = LoggerFactory.getLogger(SupportTicketServiceImpl.class);

    private final SpringDataSupportTicketRepository ticketRepository;
    private final SpringDataSupportMessageRepository messageRepository;
    private final SpringDataUserRepository userRepository;
    private final SpringDataOrderRepository orderRepository;
    private final CloudinaryService cloudinaryService;
    private final NotificationService notificationService;
    private final SupportTicketMapper ticketMapper;
    private final SupportMessageMapper messageMapper;

    public SupportTicketServiceImpl(SpringDataSupportTicketRepository ticketRepository,
                                   SpringDataSupportMessageRepository messageRepository,
                                   SpringDataUserRepository userRepository,
                                   SpringDataOrderRepository orderRepository,
                                   CloudinaryService cloudinaryService,
                                   NotificationService notificationService,
                                   SupportTicketMapper ticketMapper,
                                   SupportMessageMapper messageMapper) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.cloudinaryService = cloudinaryService;
        this.notificationService = notificationService;
        this.ticketMapper = ticketMapper;
        this.messageMapper = messageMapper;
    }

    @Override
    public SupportTicketResponse createTicket(Long userId, CreateTicketRequest request) {
        log.info("[TICKET CREATE] User {} initiating support ticket for category '{}'", userId, request.getCategory());

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        OrderEntity order = null;
        if (request.getOrderId() != null) {
            order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order", request.getOrderId()));
        }

        String ticketNumber = generateTicketNumber();

        SupportTicketEntity ticket = SupportTicketEntity.builder()
                .ticketNumber(ticketNumber)
                .user(user)
                .order(order)
                .category(request.getCategory() != null ? request.getCategory() : TicketCategory.OTHER)
                .subject(request.getSubject().trim())
                .description(request.getDescription().trim())
                .priority(request.getPriority() != null ? request.getPriority() : TicketPriority.MEDIUM)
                .status(TicketStatus.OPEN)
                .createdBy(user)
                .updatedBy(user)
                .build();

        SupportTicketEntity savedTicket = ticketRepository.save(ticket);
        log.info("[TICKET CREATED] Successfully created ticket ID: {}, Number: '{}'", savedTicket.getId(), ticketNumber);

        // If an attachment is provided on ticket creation, add initial message with attachment
        if (request.getAttachmentUrl() != null && !request.getAttachmentUrl().isBlank()) {
            SupportMessageEntity initialMessage = SupportMessageEntity.builder()
                    .ticket(savedTicket)
                    .sender(user)
                    .message(request.getDescription().trim())
                    .attachmentUrl(request.getAttachmentUrl().trim())
                    .internal(false)
                    .createdBy(user)
                    .updatedBy(user)
                    .build();
            messageRepository.save(initialMessage);
        }

        // Notify user asynchronously via Phase 15A NotificationService
        sendTicketNotification(
                user.getId(),
                NotificationType.SUPPORT_TICKET_CREATED,
                "Support Ticket Logged: " + ticketNumber,
                "Your ticket '" + savedTicket.getSubject() + "' has been logged. Our support team will assist you shortly.",
                savedTicket.getId(),
                "TKT-CREATED-" + ticketNumber
        );

        long messageCount = messageRepository.countByTicketId(savedTicket.getId());
        return ticketMapper.toResponse(savedTicket, messageCount);
    }

    @Override
    @Transactional(readOnly = true)
    public SupportTicketResponse getTicketDetails(Long ticketId, Long currentUserId, boolean isStaff) {
        SupportTicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new SupportTicketNotFoundException(ticketId));

        if (!isStaff && !ticket.getUser().getId().equals(currentUserId)) {
            log.warn("[SECURITY VIOLATION] User {} attempted unauthorized access to ticket {}", currentUserId, ticketId);
            throw new UnauthorizedTicketAccessException(currentUserId, ticketId);
        }

        long messageCount = isStaff ? messageRepository.countByTicketId(ticketId)
                : messageRepository.countByTicketIdAndInternalFalse(ticketId);

        return ticketMapper.toResponse(ticket, messageCount);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> getUserTickets(Long userId, TicketStatus status, Pageable pageable) {
        Page<SupportTicketEntity> page = (status != null)
                ? ticketRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable)
                : ticketRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return page.map(t -> {
            long count = messageRepository.countByTicketIdAndInternalFalse(t.getId());
            return ticketMapper.toResponse(t, count);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> getAdminTickets(TicketCategory category, TicketStatus status, TicketPriority priority,
                                                      Long assignedToId, Long orderId, Long userId, Pageable pageable) {
        Page<SupportTicketEntity> page = ticketRepository.findByFilters(
                userId, orderId, category, status, priority, assignedToId, pageable);

        return page.map(t -> {
            long count = messageRepository.countByTicketId(t.getId());
            return ticketMapper.toResponse(t, count);
        });
    }

    @Override
    public SupportTicketResponse assignTicket(Long ticketId, Long agentUserId, Long adminUserId) {
        log.info("[TICKET ASSIGN] Assigning ticket ID {} to agent ID {}", ticketId, agentUserId);

        SupportTicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new SupportTicketNotFoundException(ticketId));

        UserEntity agent = userRepository.findById(agentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", agentUserId));

        UserEntity admin = adminUserId != null ? userRepository.findById(adminUserId).orElse(null) : null;

        ticket.setAssignedTo(agent);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticket.setUpdatedBy(admin);

        SupportTicketEntity updated = ticketRepository.save(ticket);
        log.info("[TICKET ASSIGNED] Ticket ID {} assigned to '{}'", ticketId, agent.getFullName());

        // Notify customer
        sendTicketNotification(
                ticket.getUser().getId(),
                NotificationType.SUPPORT_MESSAGE_RECEIVED,
                "Ticket Assigned: " + ticket.getTicketNumber(),
                "Your ticket has been assigned to support specialist " + agent.getFullName() + ".",
                ticket.getId(),
                "TKT-ASSIGN-CUST-" + ticket.getId() + "-" + agent.getId()
        );

        // Notify assigned agent
        sendTicketNotification(
                agent.getId(),
                NotificationType.SUPPORT_TICKET_CREATED,
                "New Ticket Assigned: " + ticket.getTicketNumber(),
                "You have been assigned support ticket: '" + ticket.getSubject() + "'.",
                ticket.getId(),
                "TKT-ASSIGN-AGENT-" + ticket.getId() + "-" + agent.getId()
        );

        long count = messageRepository.countByTicketId(updated.getId());
        return ticketMapper.toResponse(updated, count);
    }

    @Override
    public SupportTicketResponse updateStatus(Long ticketId, UpdateTicketStatusRequest request, Long currentUserId, boolean isStaff) {
        log.info("[TICKET STATUS UPDATE] Ticket ID {}, new status '{}'", ticketId, request.getStatus());

        SupportTicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new SupportTicketNotFoundException(ticketId));

        UserEntity currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        if (!isStaff) {
            // Customer can only close their own ticket
            if (!ticket.getUser().getId().equals(currentUserId)) {
                throw new UnauthorizedTicketAccessException(currentUserId, ticketId);
            }
            if (request.getStatus() != TicketStatus.CLOSED) {
                throw new InvalidTicketOperationException("Customers are only permitted to transition ticket to CLOSED status");
            }
        }

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(request.getStatus());
        ticket.setUpdatedBy(currentUser);

        Instant now = Instant.now();
        if (request.getStatus() == TicketStatus.RESOLVED && ticket.getResolvedAt() == null) {
            ticket.setResolvedAt(now);
        } else if (request.getStatus() == TicketStatus.CLOSED && ticket.getClosedAt() == null) {
            ticket.setClosedAt(now);
        }

        SupportTicketEntity updated = ticketRepository.save(ticket);

        // If resolution notes or status comment provided, record as message
        if (request.getResolutionNotes() != null && !request.getResolutionNotes().isBlank()) {
            SupportMessageEntity noteMessage = SupportMessageEntity.builder()
                    .ticket(updated)
                    .sender(currentUser)
                    .message(request.getResolutionNotes().trim())
                    .internal(false)
                    .createdBy(currentUser)
                    .updatedBy(currentUser)
                    .build();
            messageRepository.save(noteMessage);
        }

        // Notify user upon resolution or closure
        if (request.getStatus() == TicketStatus.RESOLVED) {
            sendTicketNotification(
                    ticket.getUser().getId(),
                    NotificationType.SYSTEM_ALERT,
                    "Support Ticket Resolved: " + ticket.getTicketNumber(),
                    "Your ticket has been marked as RESOLVED. If you need further help, you may reply to reopen.",
                    ticket.getId(),
                    "TKT-RESOLVED-" + ticket.getId()
            );
        } else if (request.getStatus() == TicketStatus.CLOSED) {
            sendTicketNotification(
                    ticket.getUser().getId(),
                    NotificationType.SYSTEM_ALERT,
                    "Support Ticket Closed: " + ticket.getTicketNumber(),
                    "Your ticket has been closed.",
                    ticket.getId(),
                    "TKT-CLOSED-" + ticket.getId()
            );
        }

        long count = isStaff ? messageRepository.countByTicketId(updated.getId())
                : messageRepository.countByTicketIdAndInternalFalse(updated.getId());
        return ticketMapper.toResponse(updated, count);
    }

    @Override
    public SupportTicketResponse updatePriority(Long ticketId, UpdateTicketPriorityRequest request, Long adminUserId) {
        log.info("[TICKET PRIORITY UPDATE] Ticket ID {}, new priority '{}'", ticketId, request.getPriority());

        SupportTicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new SupportTicketNotFoundException(ticketId));

        UserEntity admin = adminUserId != null ? userRepository.findById(adminUserId).orElse(null) : null;

        ticket.setPriority(request.getPriority());
        ticket.setUpdatedBy(admin);

        SupportTicketEntity updated = ticketRepository.save(ticket);
        long count = messageRepository.countByTicketId(updated.getId());
        return ticketMapper.toResponse(updated, count);
    }

    @Override
    public SupportMessageResponse addMessage(Long ticketId, AddTicketMessageRequest request, Long senderId, boolean isStaff) {
        log.info("[TICKET MESSAGE] Adding message to ticket ID {} by sender {}", ticketId, senderId);

        SupportTicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new SupportTicketNotFoundException(ticketId));

        UserEntity sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", senderId));

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new InvalidTicketOperationException("Cannot add message to a CLOSED ticket. Please reopen or create a new ticket.");
        }

        boolean internalFlag = false;
        if (isStaff) {
            internalFlag = request.isInternal();
            // If staff replies publicly to customer, set status to WAITING_FOR_CUSTOMER if currently IN_PROGRESS
            if (!internalFlag && ticket.getStatus() == TicketStatus.IN_PROGRESS) {
                ticket.setStatus(TicketStatus.WAITING_FOR_CUSTOMER);
                ticket.setUpdatedBy(sender);
            }
        } else {
            // Customer / non-staff verification
            if (!ticket.getUser().getId().equals(senderId)) {
                log.warn("[SECURITY VIOLATION] User {} attempted unauthorized message on ticket {}", senderId, ticketId);
                throw new UnauthorizedTicketAccessException(senderId, ticketId);
            }
            // If customer replies when WAITING_FOR_CUSTOMER, move back to IN_PROGRESS
            if (ticket.getStatus() == TicketStatus.WAITING_FOR_CUSTOMER) {
                ticket.setStatus(TicketStatus.IN_PROGRESS);
                ticket.setUpdatedBy(sender);
            }
        }

        SupportMessageEntity message = SupportMessageEntity.builder()
                .ticket(ticket)
                .sender(sender)
                .message(request.getMessage().trim())
                .attachmentUrl(request.getAttachmentUrl() != null ? request.getAttachmentUrl().trim() : null)
                .internal(internalFlag)
                .createdBy(sender)
                .updatedBy(sender)
                .build();

        SupportMessageEntity saved = messageRepository.save(message);

        // Update ticket's updatedAt timestamp
        ticket.setUpdatedAt(Instant.now());
        ticketRepository.save(ticket);

        // Send notifications if not an internal note
        if (!internalFlag) {
            if (isStaff) {
                // Notify ticket creator of agent response
                sendTicketNotification(
                        ticket.getUser().getId(),
                        NotificationType.SUPPORT_MESSAGE_RECEIVED,
                        "New Message on Ticket: " + ticket.getTicketNumber(),
                        "Our support team has responded: \"" + truncate(request.getMessage().trim(), 100) + "\"",
                        ticket.getId(),
                        "TKT-MSG-NOTIF-" + saved.getId()
                );
            } else {
                // Customer replied: notify assigned agent if present
                if (ticket.getAssignedTo() != null) {
                    sendTicketNotification(
                            ticket.getAssignedTo().getId(),
                            NotificationType.SUPPORT_MESSAGE_RECEIVED,
                            "Customer Reply: " + ticket.getTicketNumber(),
                            sender.getFullName() + " replied: \"" + truncate(request.getMessage().trim(), 100) + "\"",
                            ticket.getId(),
                            "TKT-MSG-NOTIF-" + saved.getId()
                    );
                }
            }
        }

        return messageMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportMessageResponse> getTicketMessages(Long ticketId, Long currentUserId, boolean isStaff, Pageable pageable) {
        SupportTicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new SupportTicketNotFoundException(ticketId));

        if (!isStaff && !ticket.getUser().getId().equals(currentUserId)) {
            throw new UnauthorizedTicketAccessException(currentUserId, ticketId);
        }

        Page<SupportMessageEntity> messages = isStaff
                ? messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId, pageable)
                : messageRepository.findByTicketIdAndInternalFalseOrderByCreatedAtAsc(ticketId, pageable);

        return messages.map(messageMapper::toResponse);
    }

    @Override
    public String uploadAttachment(String imagePayload, Long currentUserId) {
        log.info("[SUPPORT ATTACHMENT] Uploading support attachment for user {}", currentUserId);
        return cloudinaryService.uploadImage(imagePayload, "bazaarhub/support/attachments");
    }

    @Override
    @Transactional(readOnly = true)
    public TicketStatsResponse getTicketStats() {
        long open = ticketRepository.countByStatus(TicketStatus.OPEN);
        long inProgress = ticketRepository.countByStatus(TicketStatus.IN_PROGRESS);
        long waiting = ticketRepository.countByStatus(TicketStatus.WAITING_FOR_CUSTOMER);
        long resolved = ticketRepository.countByStatus(TicketStatus.RESOLVED);
        long closed = ticketRepository.countByStatus(TicketStatus.CLOSED);
        long total = open + inProgress + waiting + resolved + closed;

        return TicketStatsResponse.builder()
                .openCount(open)
                .inProgressCount(inProgress)
                .waitingForCustomerCount(waiting)
                .resolvedCount(resolved)
                .closedCount(closed)
                .totalCount(total)
                .build();
    }

    private String generateTicketNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "TKT-" + datePart + "-" + randPart;
    }

    private void sendTicketNotification(Long userId, NotificationType type, String title, String message, Long referenceId, String dedupKey) {
        try {
            notificationService.sendNotification(SendNotificationRequest.builder()
                    .userId(userId)
                    .channel(NotificationChannel.IN_APP)
                    .notificationType(type)
                    .title(title)
                    .message(message)
                    .referenceType("SUPPORT_TICKET")
                    .referenceId(referenceId)
                    .deduplicationKey(dedupKey)
                    .build());
        } catch (Exception e) {
            log.error("[TICKET NOTIFICATION ERROR] Failed to dispatch support notification to user {}: {}", userId, e.getMessage());
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
