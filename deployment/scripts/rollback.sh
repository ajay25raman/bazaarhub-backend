#!/usr/bin/env bash
# ==============================================================================
# BazaarHub Backend - Automated Rollback Script
# Usage: ./rollback.sh <environment: staging|production> [optional: target_image_tag_or_digest]
# ==============================================================================

set -euo pipefail

ENVIRONMENT="${1:-staging}"
SPECIFIED_TARGET="${2:-}"

case "${ENVIRONMENT}" in
    staging)
        DEPLOY_DIR="/opt/bazaarhub/staging"
        COMPOSE_FILE="docker-compose.staging.yml"
        APP_PORT="${STAGING_APP_PORT:-8081}"
        ;;
    production)
        DEPLOY_DIR="/opt/bazaarhub/production"
        COMPOSE_FILE="docker-compose.production.yml"
        APP_PORT="${PROD_APP_PORT:-8080}"
        ;;
    *)
        echo "ERROR: Invalid environment '${ENVIRONMENT}'. Must be 'staging' or 'production'." >&2
        exit 1
        ;;
esac

echo "=================================================="
echo "BazaarHub Rollback Procedure"
echo "Environment: ${ENVIRONMENT}"
echo "Deploy Dir:  ${DEPLOY_DIR}"
echo "=================================================="

if [[ ! -d "${DEPLOY_DIR}" ]]; then
    echo "ERROR: Deployment directory '${DEPLOY_DIR}' does not exist on this host." >&2
    exit 1
fi

cd "${DEPLOY_DIR}"

CURRENT_IMAGE_FILE="${DEPLOY_DIR}/.current_image"
PREVIOUS_IMAGE_FILE="${DEPLOY_DIR}/.previous_image"

ROLLBACK_TARGET=""

if [[ -n "${SPECIFIED_TARGET}" ]]; then
    echo "Target rollback reference specified explicitly: ${SPECIFIED_TARGET}"
    ROLLBACK_TARGET="${SPECIFIED_TARGET}"
elif [[ -f "${PREVIOUS_IMAGE_FILE}" ]]; then
    ROLLBACK_TARGET=$(cat "${PREVIOUS_IMAGE_FILE}" | tr -d '\r\n')
    echo "Identified previous healthy image reference from history: ${ROLLBACK_TARGET}"
else
    echo "ERROR: No previous image history file found at '${PREVIOUS_IMAGE_FILE}'." >&2
    echo "Cannot determine rollback target safely." >&2
    exit 1
fi

if [[ -z "${ROLLBACK_TARGET}" ]]; then
    echo "ERROR: Rollback target reference is empty. Aborting." >&2
    exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
    echo "ERROR: Docker command not found on this system." >&2
    exit 1
fi

# Dynamically derive full image reference without hardcoding repository paths
DERIVED_BASE=""
if [[ -f "${CURRENT_IMAGE_FILE}" ]]; then
    CURRENT_IMAGE_VAL=$(cat "${CURRENT_IMAGE_FILE}" | tr -d '\r\n')
    DERIVED_BASE=$(echo "${CURRENT_IMAGE_VAL}" | sed -E 's/(@sha256:[a-f0-9]+|:[^:]+)$//')
fi

IMAGE_BASE="${IMAGE_NAME:-${DERIVED_BASE:-ghcr.io/bazaarhub/bazaarhub-backend}}"

if [[ "${ROLLBACK_TARGET}" =~ ^ghcr\.io/ || "${ROLLBACK_TARGET}" =~ / ]]; then
    FULL_IMAGE="${ROLLBACK_TARGET}"
elif [[ "${ROLLBACK_TARGET}" =~ ^sha256: ]]; then
    FULL_IMAGE="${IMAGE_BASE}@${ROLLBACK_TARGET}"
else
    FULL_IMAGE="${IMAGE_BASE}:${ROLLBACK_TARGET}"
fi

echo "Evaluating rollback image candidate: ${FULL_IMAGE}"

# Check whether rollback image exists in local Docker cache first
AUTHENTICATED_HERE=false
if docker image inspect "${FULL_IMAGE}" >/dev/null 2>&1; then
    echo "Rollback image '${FULL_IMAGE}' is already present in local Docker cache. Skipping network pull."
else
    echo "Rollback image '${FULL_IMAGE}' is not present in local cache. Pulling from registry..."

    # Check for credentials in environment or .env
    ENV_GHCR_USER=""
    ENV_GHCR_TOKEN=""
    if [[ -f "${DEPLOY_DIR}/.env" ]]; then
        ENV_GHCR_USER=$(grep -E '^(GHCR_READ_USER|GHCR_USER)=' "${DEPLOY_DIR}/.env" | head -n 1 | cut -d '=' -f2- | tr -d '"'\'' ' || true)
        ENV_GHCR_TOKEN=$(grep -E '^(GHCR_READ_TOKEN|GHCR_TOKEN)=' "${DEPLOY_DIR}/.env" | head -n 1 | cut -d '=' -f2- | tr -d '"'\'' ' || true)
    fi

    REG_USER="${GHCR_READ_USER:-${GHCR_USER:-${ENV_GHCR_USER:-}}}"
    REG_TOKEN="${GHCR_READ_TOKEN:-${GHCR_TOKEN:-${ENV_GHCR_TOKEN:-}}}"

    if [[ -n "${REG_USER}" && -n "${REG_TOKEN}" ]]; then
        echo "Authenticating to GHCR as '${REG_USER}'..."
        if ! printf '%s' "${REG_TOKEN}" | docker login ghcr.io -u "${REG_USER}" --password-stdin >/dev/null 2>&1; then
            echo "ERROR: Registry authentication failed. Cannot pull rollback image '${FULL_IMAGE}'." >&2
            exit 1
        fi
        AUTHENTICATED_HERE=true
    else
        # Verify existing Docker daemon authentication
        if [[ ! -f "${HOME}/.docker/config.json" ]] || ! grep -q 'ghcr.io' "${HOME}/.docker/config.json" 2>/dev/null; then
            echo "ERROR: Rollback image '${FULL_IMAGE}' not cached locally and no GHCR credentials found to authenticate." >&2
            echo "Provide GHCR_READ_USER and GHCR_READ_TOKEN in environment or .env file." >&2
            exit 1
        fi
        echo "Using existing Docker daemon authentication for ghcr.io."
    fi

    echo "Pulling rollback image: ${FULL_IMAGE}..."
    if ! docker pull "${FULL_IMAGE}"; then
        echo "ERROR: Failed to pull rollback image '${FULL_IMAGE}' from registry." >&2
        if [[ "${AUTHENTICATED_HERE}" == "true" ]]; then
            docker logout ghcr.io >/dev/null 2>&1 || true
        fi
        exit 1
    fi

    if [[ "${AUTHENTICATED_HERE}" == "true" ]]; then
        docker logout ghcr.io >/dev/null 2>&1 || true
    fi
fi

# Verify image before deployment
if ! docker image inspect "${FULL_IMAGE}" >/dev/null 2>&1; then
    echo "ERROR: Image verification failed. '${FULL_IMAGE}' is not inspectable." >&2
    exit 1
fi

ROLLBACK_REVISION=$(docker inspect --format='{{index .Config.Labels "org.opencontainers.image.revision"}}' "${FULL_IMAGE}" 2>/dev/null || true)
if [[ -n "${ROLLBACK_REVISION}" && "${ROLLBACK_REVISION}" != "<no value>" ]]; then
    echo "Verified rollback image revision label: ${ROLLBACK_REVISION}"
fi

# Preserve failed state for post-mortem investigation without deleting images or volumes
if [[ -f "${CURRENT_IMAGE_FILE}" ]]; then
    cp "${CURRENT_IMAGE_FILE}" "${DEPLOY_DIR}/.failed_image"
    echo "Preserved failed image reference in ${DEPLOY_DIR}/.failed_image"
fi

# Deploy container via Docker Compose using verified rollback image
echo "Deploying rollback container via Docker Compose..."
export IMAGE_DEPLOY_REF="${FULL_IMAGE}"
export IMAGE_TAG="${ROLLBACK_TARGET}"
export PROD_APP_PORT="${APP_PORT}"
export STAGING_APP_PORT="${APP_PORT}"

if [[ -f "${COMPOSE_FILE}" ]]; then
    docker compose -f "${COMPOSE_FILE}" up -d --remove-orphans
elif [[ -f "docker-compose.yml" ]]; then
    docker compose up -d --remove-orphans
else
    echo "ERROR: Compose file '${COMPOSE_FILE}' not found in ${DEPLOY_DIR}." >&2
    exit 1
fi

# Verify health of rolled-back container
HEALTH_SCRIPT="${DEPLOY_DIR}/health-check.sh"
HEALTH_URL="http://127.0.0.1:${APP_PORT}/actuator/health"

echo "Verifying health of rolled-back service at ${HEALTH_URL}..."
if [[ -f "${HEALTH_SCRIPT}" ]]; then
    if ! bash "${HEALTH_SCRIPT}" "${HEALTH_URL}"; then
        echo "CRITICAL: Health check failed for rolled-back container!" >&2
        echo "Service is unhealthy. State left intact for emergency operator investigation." >&2
        exit 1
    fi
else
    sleep 10
    if ! curl -f -s -o /dev/null "${HEALTH_URL}"; then
        echo "CRITICAL: Fallback curl probe failed for rolled-back container!" >&2
        exit 1
    fi
fi

# Update state files consistently:
# .current_image is updated to the newly active rolled-back image
# .previous_image is preserved so the known-good reference is not destroyed
echo "${FULL_IMAGE}" > "${CURRENT_IMAGE_FILE}"
echo "SUCCESS: Rollback to ${FULL_IMAGE} verified healthy in ${ENVIRONMENT}."
exit 0
