# Deployment & Operations Guide

## 1. Purpose
This document outlines build packaging, containerization, environment profile configuration, and operational deployment procedures for BazaarHub.

---

## 2. Artifact Packaging
The BazaarHub backend compiles into a single, self-contained executable JAR via the Spring Boot Maven Plugin:
```bash
./mvnw clean package -DskipTests
```
The output artifact is generated at:
`target/bazaarhub-backend-0.0.1-SNAPSHOT.jar`

---

## 3. Environment Profiles
BazaarHub supports profile-based property segregation under `src/main/resources/`:
1. `application.properties`: Shared base configuration (application name, default port `8080`, actuator exposure).
2. `application-dev.properties`: Local development profile with verbose debugging logs and detailed health endpoints.
3. `application-test.properties`: Automated testing profile with dynamic port allocation (`server.port=0`).
4. `application-prod.properties` (Roadmap): Production configuration with strict logging, SSL termination, and externalized secrets.

To launch with a specific profile:
```bash
java -jar -Dspring.profiles.active=dev target/bazaarhub-backend-0.0.1-SNAPSHOT.jar
```

---

## 4. Health & Observability
Actuator endpoints allow orchestration engines (Docker, Kubernetes) to monitor application state:
- **Liveness probe**: `GET /actuator/health/liveness`
- **Readiness probe**: `GET /actuator/health/readiness`
- **General Health**: `GET /api/v1/health`
