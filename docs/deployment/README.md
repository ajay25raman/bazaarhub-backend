# Deployment & Operations Guide

## 1. Purpose
This section documents containerization, CI/CD pipelines, Hostinger VPS orchestration, environment configurations, and disaster recovery procedures for the BazaarHub backend.

---

## 2. Deployment Documentation Index

| Guide | Description & Scope |
| :--- | :--- |
| [`ci-cd-pipeline.md`](file:///Users/ajaykumar/bazaarhub-backend/docs/deployment/ci-cd-pipeline.md) | GitHub Actions CI/CD workflows, GHCR registry authentication, triggering rules, and least-privilege security permissions. |
| [`staging-deployment.md`](file:///Users/ajaykumar/bazaarhub-backend/docs/deployment/staging-deployment.md) | Staging environment specifications (`/opt/bazaarhub/staging/`), deployment workflow, and automated smoke testing. |
| [`production-deployment.md`](file:///Users/ajaykumar/bazaarhub-backend/docs/deployment/production-deployment.md) | Production release governance (`v*` tags), GitHub Environment protection rules, and deployment procedures. |
| [`rollback-strategy.md`](file:///Users/ajaykumar/bazaarhub-backend/docs/deployment/rollback-strategy.md) | Automated and manual rollback mechanics, `.previous_image` tracking, and data safety guarantees. |
| [`server-setup.md`](file:///Users/ajaykumar/bazaarhub-backend/docs/deployment/server-setup.md) | Hostinger VPS provisioning, Docker CE installation, UFW firewall hardening, and Nginx/Caddy reverse proxy SSL setup. |

---

## 3. Deployment Artifacts Map

```
deployment/
├── compose/
│   ├── docker-compose.staging.yml     # Staging Docker Compose (Port 8081)
│   └── docker-compose.production.yml  # Production Docker Compose (Port 8080)
├── docker/
│   ├── Dockerfile                     # Multi-stage Java 21 build
│   └── .dockerignore                  # Docker build context exclusions
└── scripts/
    ├── health-check.sh                # Application health probing script
    └── rollback.sh                    # Automated rollback script
```

---

## 4. Current Status
- **CI/CD Foundation**: Configured and validated.
- **Docker & Compose**: Configured with port and network isolation.
- **Remote Hostinger VPS Connection**: Pending provision of VPS IP, SSH deployment key, and DNS domain mapping.
