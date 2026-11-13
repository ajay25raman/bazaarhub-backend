# Staging Deployment Architecture & Operations Guide

## 1. Overview
The Staging environment represents a production-parity testing environment used to validate end-to-end functionality, performance, third-party webhook integrations, and client mobile builds (Customer App, Seller App, Delivery Boy App) before releasing to production.

---

## 2. Staging Environment Specification

- **Target Directory**: `/opt/bazaarhub/staging/`
- **Compose Configuration**: [`deployment/compose/docker-compose.staging.yml`](file:///Users/ajaykumar/bazaarhub-backend/deployment/compose/docker-compose.staging.yml)
- **Container Name**: `bazaarhub-staging-backend`
- **Compose Project Name**: `bazaarhub-staging`
- **Internal Loopback Binding**: `127.0.0.1:8081 -> container:8080`
- **Network**: `bazaarhub-staging-net` (bridge)
- **Active Spring Profile**: `staging,dev`
- **Public Domain**: `https://staging-api.<your-domain>` (via Nginx/Caddy reverse proxy)

---

## 3. Directory Layout on VPS (`/opt/bazaarhub/staging/`)

```
/opt/bazaarhub/staging/
├── docker-compose.staging.yml   # Copied from deployment/compose/
├── .env                        # Stored on VPS (NEVER transferred from Git/CI)
├── .current_image              # Text file storing active image tag (e.g., sha-35f1f10)
├── .previous_image             # Text file storing previous image tag for instant rollback
├── health-check.sh             # Copied from deployment/scripts/
└── rollback.sh                 # Copied from deployment/scripts/
```

---

## 4. Staging `.env` Template
Create `/opt/bazaarhub/staging/.env` directly on the VPS:

```ini
# ==============================================================================
# BazaarHub Staging Environment Configuration (/opt/bazaarhub/staging/.env)
# ==============================================================================

# Application Ports
STAGING_APP_PORT=8081

# Spring Profiles
SPRING_PROFILES_ACTIVE=staging,dev

# JVM Tuning for VPS Staging (1.5GB RAM Max)
JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError

# Logging Configuration
LOGGING_LEVEL_COM_BAZAARHUB_BACKEND=DEBUG
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB=INFO

# Future Database Connection (Phase 2)
# SPRING_DATASOURCE_URL=jdbc:mariadb://127.0.0.1:3306/bazaarhub_staging
# SPRING_DATASOURCE_USERNAME=bazaarhub_staging_user
# SPRING_DATASOURCE_PASSWORD=your_staging_db_password
```

---

## 5. Deployment Lifecycle & Execution Steps
When a PR merges into `main`, GitHub Actions triggers `.github/workflows/deploy-staging.yml`:

```
1. Image Built & Verified -> ghcr.io/<repo>:sha-<commit>
2. SCP Compose & Scripts  -> Copies compose file and scripts to /opt/bazaarhub/staging/
3. SSH Session Open       -> Connects with STAGING_SSH_KEY to Hostinger VPS
4. Registry Login         -> Authenticates to GHCR via docker login --password-stdin
5. State Tracking         -> Copies .current_image to .previous_image
6. Image Pull             -> docker pull ghcr.io/<repo>:<tag>
7. Container Update       -> docker compose -f docker-compose.staging.yml up -d --remove-orphans
8. Smoke & Health Check   -> ./health-check.sh http://127.0.0.1:8081/actuator/health
9. Success Record         -> Writes new tag to .current_image
```

### Automated Rollback Trigger
If the health check fails after 12 retries (60 seconds), the workflow invokes `./rollback.sh staging`, reinstating the container referenced in `.previous_image`, and exits with failure.

---

## 6. Manual Staging Deployment
To trigger a deployment manually from GitHub:
1. Navigate to **Actions** -> **Deploy Staging**.
2. Click **Run workflow**.
3. (Optional) Provide a specific image tag (e.g., `sha-a1b2c3d` or `staging`).
4. Click **Run workflow**.

To execute directly on the VPS via terminal:
```bash
cd /opt/bazaarhub/staging
export IMAGE_TAG="sha-35f1f10"
docker compose -f docker-compose.staging.yml up -d
./health-check.sh http://127.0.0.1:8081/actuator/health
```
