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
    }
}
