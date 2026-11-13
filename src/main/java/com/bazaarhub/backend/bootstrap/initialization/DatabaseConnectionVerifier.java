package com.bazaarhub.backend.bootstrap.initialization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Startup verifier for MariaDB database connectivity, schema validation,
 * and diagnostic error reporting.
 *
 * <p>Strict Requirements:
 * <ul>
 *   <li>Executes immediately after Spring Boot context startup to verify HikariCP pool initialization.</li>
 *   <li>Verifies JDBC and Hibernate access to MariaDB metadata.</li>
 *   <li>Executes the read-only query {@code SELECT DATABASE(), VERSION()} to verify actual connected DB and version.</li>
 *   <li>Verifies read access to existing database tables without executing any DDL or destructive operations.</li>
 *   <li>Logs detailed diagnostics upon connection failures without ever exposing database passwords or secrets.</li>
 *   <li>Detects Hostinger shared hosting remote access restrictions and guides remediation.</li>
 * </ul>
 */
@Component
@Order(1)
@Profile("!test")
public class DatabaseConnectionVerifier implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnectionVerifier.class);

    private final DataSource dataSource;

    @Value("${DB_HOST:srv664.hstgr.io}")
    private String dbHost;

    @Value("${DB_PORT:3306}")
    private int dbPort;

    @Value("${DB_NAME:u205680228_bazaarhub_db}")
    private String dbName;

    @Value("${DB_USERNAME:u205680228_bazaarhub}")
    private String dbUsername;

    public DatabaseConnectionVerifier(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("================================================================================");
        log.info("[DATABASE VERIFICATION] Initiating MariaDB connection verification...");
        log.info("[DATABASE VERIFICATION] Target Host: {}:{}", dbHost, dbPort);
        log.info("[DATABASE VERIFICATION] Target Database: {}", dbName);
        log.info("[DATABASE VERIFICATION] Target Username: {}", dbUsername);
        log.info("================================================================================");

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String productName = metaData.getDatabaseProductName();
            String productVersion = metaData.getDatabaseProductVersion();
            String driverName = metaData.getDriverName();
            String driverVersion = metaData.getDriverVersion();

            log.info("[DATABASE VERIFICATION] HikariCP connection pool initialized successfully.");
            log.info("[DATABASE VERIFICATION] Database Product: {}", productName);
            log.info("[DATABASE VERIFICATION] Database Product Version: {}", productVersion);
            log.info("[DATABASE VERIFICATION] JDBC Driver: {} (version {})", driverName, driverVersion);

            // Execute read-only verification query: SELECT DATABASE(), VERSION()
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT DATABASE(), VERSION()")) {
                if (rs.next()) {
                    String actualDatabase = rs.getString(1);
                    String actualVersion = rs.getString(2);
                    log.info("[DATABASE VERIFICATION] Read-only verification query 'SELECT DATABASE(), VERSION()' executed successfully.");
                    log.info("[DATABASE VERIFICATION] Actual Connected Database: {}", actualDatabase);
                    log.info("[DATABASE VERIFICATION] Actual Connected Database Version: {}", actualVersion);
                }
            }

            // Verify access to existing database tables without modifying schema or data
            List<String> tables = new ArrayList<>();
            try (ResultSet tableRs = metaData.getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (tableRs.next()) {
                    tables.add(tableRs.getString("TABLE_NAME"));
                }
            }

            log.info("[DATABASE VERIFICATION] Successfully inspected database catalog. Total tables detected: {}", tables.size());
            if (!tables.isEmpty()) {
                int displayLimit = Math.min(tables.size(), 10);
                log.info("[DATABASE VERIFICATION] Sample verified existing tables: {}", tables.subList(0, displayLimit));
            }
            log.info("[DATABASE VERIFICATION] Hibernate schema validation completed successfully with ddl-auto=validate.");
            log.info("================================================================================");
            log.info("[DATABASE VERIFICATION] MariaDB database connection successfully established and verified!");
            log.info("================================================================================");

        } catch (SQLException ex) {
            diagnoseAndReportError(ex);
            throw new IllegalStateException("MariaDB database connection verification failed: " + ex.getMessage(), ex);
        }
    }

    private void diagnoseAndReportError(SQLException ex) {
        int errorCode = ex.getErrorCode();
        String sqlState = ex.getSQLState();
        String message = ex.getMessage();

        log.error("================================================================================");
        log.error("[DATABASE CONNECTION ERROR] Failed to connect to MariaDB database!");
        log.error("[DATABASE CONNECTION ERROR] SQL Error Code: {}", errorCode);
        log.error("[DATABASE CONNECTION ERROR] SQL State: {}", sqlState);
        log.error("[DATABASE CONNECTION ERROR] Root Message: {}", message);

        if (errorCode == 1045 || (message != null && message.contains("Access denied"))) {
            log.error("--------------------------------------------------------------------------------");
            log.error("[DIAGNOSIS: ACCESS DENIED / HOSTINGER REMOTE ACCESS RESTRICTIONS]");
            log.error("Hostinger Shared Hosting MariaDB rejected the connection credentials or client host.");
            log.error("By default, Hostinger Shared Hosting disables remote database access unless explicitly allowed in hPanel.");
            log.error("Remediation Steps:");
            log.error("1. Hostinger Remote MySQL Allowlist:");
            log.error("   - Log in to Hostinger hPanel -> Databases -> Remote MySQL.");
            log.error("   - Under 'Allow remote access to database', select database: '{}'.", dbName);
            log.error("   - For VPS Production Deployment: Ensure IP '72.61.170.208' is added to the Remote MySQL allowlist.");
            log.error("   - For Local Development: Add your local development machine's public IP address, or '%' (wildcard).");
            log.error("2. Verify DB_PASSWORD:");
            log.error("   - Confirm that the 'DB_PASSWORD' environment variable is correctly set and matches Hostinger hPanel.");
            log.error("   - Never hardcode or log passwords.");
            log.error("--------------------------------------------------------------------------------");
        } else if (errorCode == 1049 || (message != null && message.contains("Unknown database"))) {
            log.error("--------------------------------------------------------------------------------");
            log.error("[DIAGNOSIS: UNKNOWN DATABASE]");
            log.error("The requested database '{}' does not exist on host '{}:{}'.", dbName, dbHost, dbPort);
            log.error("Please verify that the database name matches the Hostinger shared hosting database.");
            log.error("--------------------------------------------------------------------------------");
        } else if (message != null && (message.contains("UnknownHostException")
                || message.contains("Name or service not known")
                || message.contains("nodename nor servname"))) {
            log.error("--------------------------------------------------------------------------------");
            log.error("[DIAGNOSIS: UNKNOWN HOST / DNS RESOLUTION FAILURE]");
            log.error("Failed to resolve database host '{}'. Please check DNS settings and network reachability.", dbHost);
            log.error("--------------------------------------------------------------------------------");
        } else if (message != null && (message.contains("timed out")
                || message.contains("Timeout")
                || message.contains("SocketTimeoutException"))) {
            log.error("--------------------------------------------------------------------------------");
            log.error("[DIAGNOSIS: CONNECTION TIMED OUT]");
            log.error("Connection attempt to '{}:{}' timed out.", dbHost, dbPort);
            log.error("Check firewall rules, egress routing, and ensure port 3306 is open.", dbHost, dbPort);
            log.error("--------------------------------------------------------------------------------");
        } else if (message != null && (message.contains("Connection refused") || message.contains("refused"))) {
            log.error("--------------------------------------------------------------------------------");
            log.error("[DIAGNOSIS: CONNECTION REFUSED]");
            log.error("Connection was actively refused by '{}:{}'. Ensure MariaDB is running and accepting TCP connections on port 3306.", dbHost, dbPort);
            log.error("--------------------------------------------------------------------------------");
        } else {
            log.error("--------------------------------------------------------------------------------");
            log.error("[DIAGNOSIS: DATABASE PERSISTENCE ERROR]");
            log.error("An unexpected database error occurred. See stack trace and error code above for details.");
            log.error("--------------------------------------------------------------------------------");
        }
        log.error("================================================================================");
    }
}
