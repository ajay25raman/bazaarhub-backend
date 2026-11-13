package com.bazaarhub.backend.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.*;
import java.util.*;

/**
 * Diagnostic read-only inspection against live Hostinger MariaDB.
 */
class PostRbacSeedLiveVerificationTest {

    private static final Logger log = LoggerFactory.getLogger(PostRbacSeedLiveVerificationTest.class);

    private HikariDataSource createDataSource() throws Exception {
        Properties properties = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                properties.load(in);
            }
        }

        String dbHost = System.getenv().getOrDefault("DB_HOST", properties.getProperty("DB_HOST", "srv664.hstgr.io"));
        String dbPort = System.getenv().getOrDefault("DB_PORT", properties.getProperty("DB_PORT", "3306"));
        String dbName = System.getenv().getOrDefault("DB_NAME", properties.getProperty("DB_NAME", "u205680228_bazaarhub_db"));
        String dbUser = System.getenv().getOrDefault("DB_USERNAME", properties.getProperty("DB_USERNAME", "u205680228_bazaarhub"));
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "Bazaar@Hub25");

        HikariConfig config = new HikariConfig();
        config.setPoolName("LiveRbacVerificationPool");
        config.setDriverClassName("org.mariadb.jdbc.Driver");
        config.setJdbcUrl("jdbc:mariadb://" + dbHost + ":" + dbPort + "/" + dbName + "?serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8");
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(2);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(15000);
        config.setValidationTimeout(5000);

        return new HikariDataSource(config);
    }

    @Test
    @DisplayName("Inspect live Hostinger MariaDB RBAC tables and log all results")
    void inspectLiveRbacTables() throws Exception {
        String dbPassword = System.getenv("DB_PASSWORD");
        org.junit.jupiter.api.Assumptions.assumeTrue(dbPassword != null && !dbPassword.isBlank(),
                "Skipping live database connection: DB_PASSWORD environment variable not set");

        try (HikariDataSource ds = createDataSource();
             Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {

            log.info("[LIVE INSPECTION] Database Connection: SUCCESS");

            // Roles
            List<Map<String, Object>> roles = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery("SELECT id, name, description, created_at, updated_at FROM roles")) {
                while (rs.next()) {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("id", rs.getLong("id"));
                    r.put("name", rs.getString("name"));
                    r.put("description", rs.getString("description"));
                    roles.add(r);
                }
            }
            log.info("[LIVE INSPECTION] 'roles' count: {}, rows: {}", roles.size(), roles);

            // Permissions count
            int permCount = 0;
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM permissions")) {
                if (rs.next()) permCount = rs.getInt(1);
            }
            log.info("[LIVE INSPECTION] 'permissions' count: {}", permCount);

            // Role_permissions count
            int rpCount = 0;
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM role_permissions")) {
                if (rs.next()) rpCount = rs.getInt(1);
            }
            log.info("[LIVE INSPECTION] 'role_permissions' count: {}", rpCount);

            // User_roles count
            int urCount = 0;
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM user_roles")) {
                if (rs.next()) urCount = rs.getInt(1);
            }
            log.info("[LIVE INSPECTION] 'user_roles' count: {}", urCount);

            // Users count
            int usersCount = 0;
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next()) usersCount = rs.getInt(1);
            }
            log.info("[LIVE INSPECTION] 'users' count: {}", usersCount);

            // Database catalog & version
            try (ResultSet rs = stmt.executeQuery("SELECT DATABASE(), CURRENT_USER(), VERSION()")) {
                if (rs.next()) {
                    log.info("[LIVE INSPECTION] Connected Database: {}, Current User: {}, Version: {}",
                            rs.getString(1), rs.getString(2), rs.getString(3));
                }
            }
        }
    }
}
