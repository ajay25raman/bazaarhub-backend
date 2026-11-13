package com.bazaarhub.backend.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration verification test for MariaDB database connectivity,
 * connection pooling with HikariCP, and read-only schema inspection.
 */
class DatabaseConnectionVerificationTest {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnectionVerificationTest.class);

    @Test
    @DisplayName("Verify production database properties in application.properties")
    void testDatabasePropertiesConfiguration() throws Exception {
        Properties properties = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            assertNotNull(in, "application.properties must exist in classpath");
            properties.load(in);
        }

        assertEquals("org.mariadb.jdbc.Driver", properties.getProperty("spring.datasource.driver-class-name"),
                "Driver class must be MariaDB driver");
        assertTrue(properties.getProperty("spring.datasource.url").contains("jdbc:mariadb://"),
                "Datasource URL must use MariaDB JDBC protocol");
        assertEquals("validate", properties.getProperty("spring.jpa.hibernate.ddl-auto"),
                "Hibernate ddl-auto must be strictly set to 'validate'");
        assertEquals("false", properties.getProperty("spring.jpa.open-in-view"),
                "Open Session In View must be disabled");
        assertEquals("org.hibernate.dialect.MariaDBDialect", properties.getProperty("spring.jpa.properties.hibernate.dialect"),
                "Hibernate dialect must be MariaDBDialect");
        assertEquals("UTC", properties.getProperty("spring.jpa.properties.hibernate.jdbc.time_zone"),
                "Hibernate JDBC timezone must be UTC");
        assertEquals("BazaarHubHikariCP", properties.getProperty("spring.datasource.hikari.pool-name"),
                "HikariCP pool name must be configured");
    }

    @Test
    @DisplayName("Verify live MariaDB connectivity, HikariCP, database name, and version")
    void testLiveMariaDBConnectivity() {
        String dbPassword = System.getenv("DB_PASSWORD");
        assumeTrue(dbPassword != null && !dbPassword.isBlank(),
                "Skipping live database connection test: DB_PASSWORD environment variable not set");

        String dbHost = System.getenv().getOrDefault("DB_HOST", "srv664.hstgr.io");
        String dbPort = System.getenv().getOrDefault("DB_PORT", "3306");
        String dbName = System.getenv().getOrDefault("DB_NAME", "u205680228_bazaarhub_db");
        String dbUser = System.getenv().getOrDefault("DB_USERNAME", "u205680228_bazaarhub");

        HikariConfig config = new HikariConfig();
        config.setPoolName("BazaarHubVerificationPool");
        config.setDriverClassName("org.mariadb.jdbc.Driver");
        config.setJdbcUrl("jdbc:mariadb://" + dbHost + ":" + dbPort + "/" + dbName + "?serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8");
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(2);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(15000);
        config.setValidationTimeout(5000);

        try (HikariDataSource dataSource = new HikariDataSource(config);
             Connection connection = dataSource.getConnection()) {

            assertNotNull(connection, "JDBC connection must not be null");
            assertFalse(connection.isClosed(), "Connection must be active and open");

            DatabaseMetaData metaData = connection.getMetaData();
            String productName = metaData.getDatabaseProductName();
            String productVersion = metaData.getDatabaseProductVersion();

            log.info("[LIVE TEST] Connected to Database Product: {}", productName);
            log.info("[LIVE TEST] Connected Database Product Version: {}", productVersion);
            assertTrue(productName.toLowerCase().contains("mariadb"), "Product must be MariaDB");

            // Execute read-only query: SELECT DATABASE(), VERSION()
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT DATABASE(), VERSION()")) {
                assertTrue(rs.next(), "SELECT DATABASE(), VERSION() must return 1 row");
                String currentDb = rs.getString(1);
                String currentVersion = rs.getString(2);

                log.info("[LIVE TEST] Executed 'SELECT DATABASE(), VERSION()'");
                log.info("[LIVE TEST] Actual Database Name: {}", currentDb);
                log.info("[LIVE TEST] Actual MariaDB Version: {}", currentVersion);

                assertEquals(dbName, currentDb, "Connected database name must match expected database");
                assertNotNull(currentVersion, "Version must not be null");
            }

            // Inspect tables without altering schema
            List<String> tables = new ArrayList<>();
            try (ResultSet tableRs = metaData.getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (tableRs.next()) {
                    tables.add(tableRs.getString("TABLE_NAME"));
                }
            }

            log.info("[LIVE TEST] Inspected schema. Detected table count: {}", tables.size());
            log.info("[LIVE TEST] Table list: {}", tables);

            // Read-only inspection of existing roles
            List<String> existingRoles = new ArrayList<>();
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT name FROM roles")) {
                while (rs.next()) {
                    existingRoles.add(rs.getString("name"));
                }
            }
            log.info("[LIVE TEST] Existing roles in 'roles' table: {}", existingRoles);

        } catch (Exception ex) {
            log.error("[LIVE TEST ERROR] MariaDB connectivity test failed: {}", ex.getMessage());
            Throwable cause = ex;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            if (cause instanceof SQLException sqlEx) {
                log.error("[LIVE TEST ERROR] Root SQL Error Code: {}, SQL State: {}", sqlEx.getErrorCode(), sqlEx.getSQLState());
                if (sqlEx.getErrorCode() == 1045) {
                    log.error("[LIVE TEST DIAGNOSIS] Hostinger Remote MySQL restrictions active. The client IP is not allowlisted in Hostinger hPanel -> Remote MySQL.");
                    log.error("[LIVE TEST DIAGNOSIS] Action: Allowlist VPS IP 72.61.170.208 for production, or local IP / wildcard '%' for development.");
                }
            }
            throw new AssertionError("Failed to connect to MariaDB: " + ex.getMessage(), ex);
        }
    }
}
