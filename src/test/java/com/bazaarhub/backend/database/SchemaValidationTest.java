package com.bazaarhub.backend.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=validate"
})
class SchemaValidationTest {

    @Autowired(required = false)
    private EntityManager entityManager;

    @Test
    @DisplayName("Verify Hibernate schema validation against MariaDB with ddl-auto=validate")
    void testHibernateSchemaValidation() {
        assertNotNull(entityManager, "EntityManager must be initialized and schema validated");
        Set<EntityType<?>> entities = entityManager.getMetamodel().getEntities();
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("UserEntity")), "UserEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("RoleEntity")), "RoleEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("PermissionEntity")), "PermissionEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("UserDeviceEntity")), "UserDeviceEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("InventoryEntity")), "InventoryEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("InventoryMovementEntity")), "InventoryMovementEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("StockReservationEntity")), "StockReservationEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("CustomerProfileEntity")), "CustomerProfileEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("AddressEntity")), "AddressEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("WishlistEntity")), "WishlistEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("WishlistItemEntity")), "WishlistItemEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("CartEntity")), "CartEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("CartItemEntity")), "CartItemEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("OrderEntity")), "OrderEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("SellerOrderEntity")), "SellerOrderEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("OrderItemEntity")), "OrderItemEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("OrderStatusHistoryEntity")), "OrderStatusHistoryEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("PaymentEntity")), "PaymentEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("PaymentTransactionEntity")), "PaymentTransactionEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("RefundEntity")), "RefundEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("DeliveryBoyEntity")), "DeliveryBoyEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("ShipmentEntity")), "ShipmentEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("DeliveryAssignmentEntity")), "DeliveryAssignmentEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("DeliveryTrackingEntity")), "DeliveryTrackingEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("ProductReviewEntity")), "ProductReviewEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("ReviewImageEntity")), "ReviewImageEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("NotificationEntity")), "NotificationEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("NotificationTemplateEntity")), "NotificationTemplateEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("SupportTicketEntity")), "SupportTicketEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("SupportMessageEntity")), "SupportMessageEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("AiConversationEntity")), "AiConversationEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("AiMessageEntity")), "AiMessageEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("AiUsageLogEntity")), "AiUsageLogEntity must be registered");
        assertTrue(entities.stream().anyMatch(e -> e.getName().equals("AnalyticsEventEntity")), "AnalyticsEventEntity must be registered");
    }
}
