# Production Deployment Architecture & Operations Guide

## 1. Overview
The Production environment delivers high-availability, secure, and performant backend services to active BazaarHub buyers, vendors, delivery partners, and administrators.

Production deployments are strictly governed by formal release tags, immutable container artifacts, mandatory health check verification, and zero-downtime rollback procedures.

---

## 2. Production Environment Specification

- **Target Directory**: `/opt/bazaarhub/production/`
- **Compose Configuration**: [`deployment/compose/docker-compose.production.yml`](file:///Users/ajaykumar/bazaarhub-backend/deployment/compose/docker-compose.production.yml)
- **Container Name**: `bazaarhub-production-backend`
- **Compose Project Name**: `bazaarhub-production`
- **Internal Loopback Binding**: `127.0.0.1:8080 -> container:8080`
- **Network**: `bazaarhub-production-net` (bridge)
- **Active Spring Profile**: `prod`
- **Public Domain**: `https://api.<your-domain>` (via Nginx/Caddy reverse proxy)

---

## 3. Directory Layout on VPS (`/opt/bazaarhub/production/`)

```
/opt/bazaarhub/production/
├── docker-compose.production.yml # Copied from deployment/compose/
├── .env                         # Stored on VPS with production secrets (chmod 600)
├── .current_image               # Text file storing active release tag (e.g., v1.0.0)
├── .previous_image              # Text file storing previous working release tag
├── health-check.sh              # Copied from deployment/scripts/
└── rollback.sh                  # Copied from deployment/scripts/
```

---

## 4. Production `.env` Template
Create `/opt/bazaarhub/production/.env` on the VPS with permissions `chmod 600`:

```ini
# ==============================================================================
# BazaarHub Production Environment Configuration (/opt/bazaarhub/production/.env)
# Permissions: chmod 600 /opt/bazaarhub/production/.env
# ==============================================================================

# Application Port
PROD_APP_PORT=8080

# Spring Active Profile
SPRING_PROFILES_ACTIVE=prod

# Production JVM Tuning (2GB+ RAM Allocation)
JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -XX:+UseG1GC

# Actuator Security in Production
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info
MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=never

# Future Production Database (Phase 2)
# SPRING_DATASOURCE_URL=jdbc:mariadb://127.0.0.1:3306/bazaarhub_prod?useSSL=true&serverTimezone=UTC
# SPRING_DATASOURCE_USERNAME=bazaarhub_prod_user
# SPRING_DATASOURCE_PASSWORD=strong_production_generated_password

# Future JWT Secrets (Phase 3)
# BAZAARHUB_SECURITY_JWT_SECRET=secure_random_base64_256bit_token
```

---

## 5. Deployment Governance & Protection Rules

### 1. Release Tag Trigger
Production deployment is triggered exclusively by pushing an annotated release tag matching `v*` (e.g., `v1.0.0`, `v1.0.1`) or via manual dispatch with an explicit release tag. Arbitrary feature branches cannot deploy to production.

```bash
# Example: Creating and pushing a production release
git tag -a v1.0.0 -m "Release v1.0.0: Modular Monolith Clean Architecture Foundation"
git push origin v1.0.0
```

### 2. GitHub Environment Protection (Required Reviewers)
The workflow references the GitHub Environment named `production`:
```yaml
environment: production
```
> **Enterprise Setting Note**: In GitHub Enterprise, Pro, or Team organizations, navigate to **Settings -> Environments -> production** and enable **Required reviewers**. When configured, GitHub will pause the workflow and request manual sign-off from designated senior reviewers before SSH commands can touch the production VPS.

---

## 6. Execution Lifecycle
When a release tag is pushed:

1. **Pre-Flight Validation**: Validates semver format `vX.Y.Z`.
2. **Checkout Release**: Checks out the exact Git commit corresponding to the tag.
3. **Copy Configuration**: Syncs `docker-compose.production.yml` and operational scripts.
4. **Environment Check**: Verifies `/opt/bazaarhub/production/.env` exists on the host. If missing, execution halts immediately before modifying containers.
5. **Image Verification & Pull**: Pulls `ghcr.io/<repo>:<tag>`.
6. **Container Replacement**: Executes `docker compose -f docker-compose.production.yml up -d --remove-orphans`.
7. **Health Probe**: Runs `health-check.sh` against `http://127.0.0.1:8080/actuator/health`.
8. **Automated Rollback on Failure**: If health checks fail within 75 seconds (15 retries), `./rollback.sh production` is immediately triggered.
9. **State Confirmation**: Active release tag is recorded into `.current_image`.
