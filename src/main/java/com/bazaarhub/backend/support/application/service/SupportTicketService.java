package com.bazaarhub.backend.support.application.service;

import com.bazaarhub.backend.support.application.dto.request.*;
import com.bazaarhub.backend.support.application.dto.response.SupportMessageResponse;
import com.bazaarhub.backend.support.application.dto.response.SupportTicketResponse;
import com.bazaarhub.backend.support.application.dto.response.TicketStatsResponse;
import com.bazaarhub.backend.support.domain.model.TicketCategory;
import com.bazaarhub.backend.support.domain.model.TicketPriority;
import com.bazaarhub.backend.support.domain.model.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupportTicketService {

    SupportTicketResponse createTicket(Long userId, CreateTicketRequest request);

    SupportTicketResponse getTicketDetails(Long ticketId, Long currentUserId, boolean isStaff);

    Page<SupportTicketResponse> getUserTickets(Long userId, TicketStatus status, Pageable pageable);

    Page<SupportTicketResponse> getAdminTickets(TicketCategory category, TicketStatus status, TicketPriority priority,
                                               Long assignedToId, Long orderId, Long userId, Pageable pageable);

    SupportTicketResponse assignTicket(Long ticketId, Long agentUserId, Long adminUserId);

    SupportTicketResponse updateStatus(Long ticketId, UpdateTicketStatusRequest request, Long currentUserId, boolean isStaff);

    SupportTicketResponse updatePriority(Long ticketId, UpdateTicketPriorityRequest request, Long adminUserId);

    SupportMessageResponse addMessage(Long ticketId, AddTicketMessageRequest request, Long senderId, boolean isStaff);

    Page<SupportMessageResponse> getTicketMessages(Long ticketId, Long currentUserId, boolean isStaff, Pageable pageable);

    String uploadAttachment(String imagePayload, Long currentUserId);

    TicketStatsResponse getTicketStats();
}
