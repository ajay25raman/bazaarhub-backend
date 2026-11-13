#!/usr/bin/env bash
# ==============================================================================
# BazaarHub Backend - Application Health & Smoke Test Script
#
# Authentication Notice:
# By default, Spring Security protects all endpoints including /actuator/health
# unless explicitly permitted in SecurityFilterChain or if HTTP Basic credentials
# are supplied. This script supports optional authentication via HEALTH_AUTH
# (format: "username:password" or "Bearer <token>") while strictly redacting
# and preventing credential exposure in logs and console output.
# ==============================================================================

set -euo pipefail

# Configurable Parameters
HEALTH_URL="${1:-${HEALTH_URL:-http://127.0.0.1:8080/actuator/health}}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-12}"
SLEEP_SECONDS="${SLEEP_SECONDS:-5}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-5}"
HEALTH_AUTH="${HEALTH_AUTH:-}"

echo "=================================================="
echo "BazaarHub Backend - Health Check Initiated"
echo "Target URL:       ${HEALTH_URL}"
echo "Max Attempts:     ${MAX_ATTEMPTS}"
echo "Timeout Per Try:  ${TIMEOUT_SECONDS}s"
if [[ -n "${HEALTH_AUTH}" ]]; then
    echo "Authentication:   Configured (Credentials Masked)"
else
    echo "Authentication:   None / Public Endpoint Expected"
fi
echo "=================================================="

attempt=1
while [[ ${attempt} -le ${MAX_ATTEMPTS} ]]; do
    echo "[Attempt ${attempt}/${MAX_ATTEMPTS}] Probing health endpoint..."
    
    # Construct curl command with optional credentials safely
    HTTP_CODE=""
    if [[ -n "${HEALTH_AUTH}" ]]; then
        # Check if auth is Bearer token or Basic user:pass
        if [[ "${HEALTH_AUTH}" =~ ^Bearer ]]; then
            HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
                --connect-timeout "${TIMEOUT_SECONDS}" \
                --max-time "${TIMEOUT_SECONDS}" \
                -H "Authorization: ${HEALTH_AUTH}" \
                "${HEALTH_URL}" 2>/dev/null || echo "000")
        else
            HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
                --connect-timeout "${TIMEOUT_SECONDS}" \
                --max-time "${TIMEOUT_SECONDS}" \
                -u "${HEALTH_AUTH}" \
                "${HEALTH_URL}" 2>/dev/null || echo "000")
        fi
    else
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
            --connect-timeout "${TIMEOUT_SECONDS}" \
            --max-time "${TIMEOUT_SECONDS}" \
            "${HEALTH_URL}" 2>/dev/null || echo "000")
    fi

    if [[ "${HTTP_CODE}" == "200" ]]; then
        echo "SUCCESS: Service responded with HTTP ${HTTP_CODE} (Healthy)."
        exit 0
    elif [[ "${HTTP_CODE}" == "401" || "${HTTP_CODE}" == "403" ]]; then
        echo "WARNING: Received HTTP ${HTTP_CODE} (Unauthorized/Forbidden)."
        echo "Authentication is required by Spring Security. Ensure HEALTH_AUTH is configured or endpoints are permitted."
    else
        echo "Status: HTTP ${HTTP_CODE} (Not ready yet). Retrying in ${SLEEP_SECONDS}s..."
    fi

    attempt=$((attempt + 1))
    sleep "${SLEEP_SECONDS}"
done

echo "ERROR: Health check failed after ${MAX_ATTEMPTS} attempts against ${HEALTH_URL}."
exit 1
