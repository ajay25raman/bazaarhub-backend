package com.bazaarhub.backend.ai;

import com.bazaarhub.backend.ai.application.dto.request.AiChatRequest;
import com.bazaarhub.backend.ai.application.dto.response.AiChatResponse;
import com.bazaarhub.backend.ai.application.service.AiSupportAssistantServiceImpl;
import com.bazaarhub.backend.ai.domain.exception.AiDisabledException;
import com.bazaarhub.backend.ai.domain.exception.UnauthorizedAiAccessException;
import com.bazaarhub.backend.ai.infrastructure.client.AiClient;
import com.bazaarhub.backend.ai.infrastructure.client.AiCompletionResponse;
import com.bazaarhub.backend.ai.infrastructure.config.AiConfigurationProperties;
import com.bazaarhub.backend.ai.infrastructure.persistence.entity.AiConversationEntity;
import com.bazaarhub.backend.ai.infrastructure.persistence.entity.AiMessageEntity;
import com.bazaarhub.backend.ai.infrastructure.persistence.repository.SpringDataAiConversationRepository;
import com.bazaarhub.backend.ai.infrastructure.persistence.repository.SpringDataAiMessageRepository;
import com.bazaarhub.backend.ai.infrastructure.security.AiPromptSanitizer;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.order.domain.model.OrderStatus;
import com.bazaarhub.backend.order.domain.model.PaymentStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiSupportAssistantServiceUnitTest {

    @Mock
    private AiClient aiClient;

    @Spy
    private AiPromptSanitizer promptSanitizer = new AiPromptSanitizer();

    @Mock
    private SpringDataOrderRepository orderRepository;

    @Mock
    private SpringDataUserRepository userRepository;

    @Mock
    private SpringDataAiConversationRepository conversationRepository;

    @Mock
    private SpringDataAiMessageRepository messageRepository;

    @InjectMocks
    private AiSupportAssistantServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(aiClient.isEnabled()).thenReturn(true);
        lenient().when(aiClient.getProviderName()).thenReturn("Groq");
        lenient().when(aiClient.getModelName()).thenReturn("openai/gpt-oss-20b");
    }

    @Test
    @DisplayName("Should throw AiDisabledException when AI is turned off")
    void shouldThrowWhenAiDisabled() {
        when(aiClient.isEnabled()).thenReturn(false);

        AiChatRequest request = AiChatRequest.builder()
                .message("Hello")
                .build();

        assertThrows(AiDisabledException.class, () -> service.chat(request, 100L));
    }

    @Test
    @DisplayName("Should answer guest inquiry successfully")
    void shouldAnswerGuestInquiry() {
        AiChatRequest request = AiChatRequest.builder()
                .message("What is BazaarHub's return policy?")
                .build();

        when(aiClient.completeChat(anyList(), anyInt(), anyDouble()))
                .thenReturn(AiCompletionResponse.builder()
                        .content("Our return window is 30 days.")
                        .model("openai/gpt-oss-20b")
                        .promptTokens(100)
                        .completionTokens(20)
                        .totalTokens(120)
                        .latencyMs(150)
                        .build());

        AiChatResponse response = service.chat(request, null);

        assertNotNull(response);
        assertEquals("Our return window is 30 days.", response.getReply());
        verify(aiClient, times(1)).completeChat(anyList(), anyInt(), anyDouble());
    }

    @Test
    @DisplayName("Should allow customer to query own order")
    void shouldAllowCustomerToQueryOwnOrder() {
        Long customerId = 10L;
        Long orderId = 50L;
        String orderNumber = "ORD-2026-999";

        AiChatRequest request = AiChatRequest.builder()
                .orderId(orderId)
                .message("What is the status of my order?")
                .build();

        UserEntity user = UserEntity.builder().id(customerId).build();
        OrderEntity order = OrderEntity.builder()
                .id(orderId)
                .orderNumber(orderNumber)
                .user(user)
                .status(OrderStatus.SHIPPED)
                .paymentStatus(PaymentStatus.PAID)
                .grandTotal(new BigDecimal("150.00"))
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userRepository.findById(customerId)).thenReturn(Optional.of(user));

        when(conversationRepository.save(any(AiConversationEntity.class))).thenAnswer(inv -> {
            AiConversationEntity entity = inv.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        when(messageRepository.save(any(AiMessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        when(aiClient.completeChat(anyList(), anyInt(), anyDouble()))
                .thenReturn(AiCompletionResponse.builder()
                        .content("Your order ORD-2026-999 is shipped.")
                        .model("openai/gpt-oss-20b")
                        .promptTokens(120)
                        .completionTokens(25)
                        .totalTokens(145)
                        .latencyMs(180)
                        .build());

        AiChatResponse response = service.chat(request, customerId);

        assertNotNull(response);
        assertTrue(response.getReply().contains("shipped"));
    }

    @Test
    @DisplayName("Should reject inquiry when customer queries someone else's order")
    void shouldRejectWhenCustomerQueriesAnotherUserOrder() {
        Long attackerId = 10L;
        Long victimId = 99L;
        Long orderId = 50L;

        AiChatRequest request = AiChatRequest.builder()
                .orderId(orderId)
                .message("Track order please")
                .build();

        UserEntity victim = UserEntity.builder().id(victimId).build();
        OrderEntity order = OrderEntity.builder()
                .id(orderId)
                .orderNumber("ORD-2026-VICTIM")
                .user(victim)
                .status(OrderStatus.CONFIRMED)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(UnauthorizedAiAccessException.class, () -> service.chat(request, attackerId));
    }
}
