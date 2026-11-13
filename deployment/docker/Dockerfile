# ==============================================================================
# Multi-Stage Dockerfile for BazaarHub Backend (Modular Monolith)
# Runtime: Java 21 LTS (Eclipse Temurin)
# Framework: Spring Boot 4.1.2
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build Stage
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS builder

LABEL maintainer="BazaarHub DevOps Team"
LABEL description="Build environment for BazaarHub Spring Boot Backend"

WORKDIR /workspace

# Install essential build utilities
RUN apt-get update && \
    apt-get install -y --no-install-recommends bash ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# Copy Maven Wrapper and POM first to leverage Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Ensure maven wrapper executable permissions
RUN chmod +x ./mvnw

# Pre-fetch project dependencies offline
RUN ./mvnw --batch-mode dependency:go-offline -B || true

# Copy application source code
COPY src ./src

# Compile and package application (tests executed and verified in CI prior to container build)
RUN ./mvnw --batch-mode clean package -DskipTests && \
    cp target/bazaarhub-backend-*.jar app.jar

# ------------------------------------------------------------------------------
# Stage 2: Runtime Stage
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runner

LABEL org.opencontainers.image.title="BazaarHub Backend"
LABEL org.opencontainers.image.description="Enterprise AI-Powered Multi-Vendor Marketplace Backend"
LABEL org.opencontainers.image.vendor="BazaarHub"
LABEL org.opencontainers.image.source="https://github.com/bazaarhub-backend"

# Install curl for container health checks
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl ca-certificates tzdata && \
    rm -rf /var/lib/apt/lists/*

# Security: Create dedicated non-root system group and user
RUN groupadd -g 10001 bazaarhub && \
    useradd -u 10001 -g bazaarhub -s /bin/bash -m -d /home/bazaarhub bazaarhub

WORKDIR /app

# Copy the built artifact from builder stage with strict ownership
COPY --from=builder --chown=bazaarhub:bazaarhub /workspace/app.jar /app/app.jar

# Enforce non-root execution
USER bazaarhub:bazaarhub

# Expose internal application port
EXPOSE 8080

# Environment variables for JVM container tuning (customizable at runtime)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE="prod"

# Container Healthcheck Strategy
# Note: Actuator /health or /api/v1/health status code check.
# If default Spring Security is active without public permitAll, credentials or anonymous permit configuration is required.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f -s -o /dev/null http://localhost:8080/actuator/health || exit 1

# Launch application using exec form to properly receive OS signals (SIGTERM/SIGINT)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
