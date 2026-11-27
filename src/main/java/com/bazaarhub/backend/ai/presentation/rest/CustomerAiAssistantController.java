package com.bazaarhub.backend.ai.presentation.rest;

import com.bazaarhub.backend.ai.application.dto.request.AiChatRequest;
import com.bazaarhub.backend.ai.application.dto.response.AiChatResponse;
import com.bazaarhub.backend.ai.application.service.AiSupportAssistantService;
import com.bazaarhub.backend.ai.infrastructure.persistence.entity.AiConversationEntity;
import com.bazaarhub.backend.ai.infrastructure.persistence.entity.AiMessageEntity;
import com.bazaarhub.backend.ai.infrastructure.persistence.repository.SpringDataAiConversationRepository;
import com.bazaarhub.backend.ai.infrastructure.persistence.repository.SpringDataAiMessageRepository;
import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/ai")
@Tag(name = "Customer AI Assistant", description = "Endpoints for authenticated customer shopping, order status inquiry, and AI support")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
public class CustomerAiAssistantController {

    private final AiSupportAssistantService assistantService;
    private final SpringDataAiConversationRepository conversationRepository;
    private final SpringDataAiMessageRepository messageRepository;

    public CustomerAiAssistantController(AiSupportAssistantService assistantService,
                                         SpringDataAiConversationRepository conversationRepository,
                                         SpringDataAiMessageRepository messageRepository) {
        this.assistantService = assistantService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with BazaarHub AI Shopping & Support Assistant", description = "Query orders, shipments, policies, or marketplace help with verified customer ownership checks")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "AI response generated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid message content", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires CUSTOMER or ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "AI assistant inference error", content = @Content)
    })
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AiChatRequest request) {

        AiChatResponse response = assistantService.chat(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("AI response generated", response));
    }

    @GetMapping("/conversations")
    @Operation(summary = "List customer's AI conversation history", description = "Paginated list of previous conversational support sessions")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conversation history retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires CUSTOMER or ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<AiConversationEntity>>> listConversations(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<AiConversationEntity> conversations = conversationRepository.findByUserIdOrderByLastMessageAtDesc(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok("Conversations retrieved", conversations));
    }

    @GetMapping("/conversations/{id}/messages")
    @Operation(summary = "Retrieve messages in a conversation session", description = "Ordered chronological list of messages in a specific session")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conversation messages retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires CUSTOMER or ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Conversation session not found or belongs to another user", content = @Content)
    })
    public ResponseEntity<ApiResponse<List<AiMessageEntity>>> getConversationMessages(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Conversation ID", example = "1") @PathVariable Long id) {

        AiConversationEntity conversation = conversationRepository.findByIdAndUserId(id, principal.getId())
                .orElse(null);
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }

        List<AiMessageEntity> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(id);
        return ResponseEntity.ok(ApiResponse.ok("Messages retrieved", messages));
    }
}
