# BazaarHub CI/CD Pipeline Architecture

## 1. Overview & Architectural Philosophy
The BazaarHub continuous integration and continuous deployment (CI/CD) engine is designed to deliver automated, verifiable, and secure releases of the Modular Monolith backend to Hostinger VPS environments.

The pipeline implements three core enterprise tenets:
1. **Build Once, Deploy Everywhere**: Exactly one immutable container image is built per commit/tag, verified, pushed to GitHub Container Registry (GHCR), and promoted through Staging to Production. No secondary builds are performed for different environments.
2. **Shift-Left Verification**: Pull requests are validated with comprehensive compilation, unit testing, and static checks prior to merging. Unverified code cannot generate container images.
3. **Least Privilege & Zero-Secret Exposure**: GitHub Actions workflows utilize minimal required permissions (`contents: read`, `packages: write`), and never handle or transmit production database credentials or `.env` files.

---

## 2. Pipeline Topography & Workflow Matrix

```
                      +------------------------------------------+
                      |         Developer Git Activity           |
                      +------------------------------------------+
                                     |            |
                    Pull Request to main          Push to main / Tag v*
                                     |            |
                                     v            v
                      +------------------+    +-----------------------+
                      |  backend-ci.yml  |    |  build-and-push.yml   |
                      | (Maven verify)   |    | (Verify + Build Image)|
                      +------------------+    +-----------------------+
                                                         |
                                                  Pushes to GHCR
                                          (ghcr.io/bazaarhub/bazaarhub-backend)
                                                         |
                              +--------------------------+--------------------------+
                              |                                                     |
                    On main branch build                                  On v* Release Tag
                              |                                                     |
                              v                                                     v
                 +--------------------------+                         +--------------------------+
                 |   deploy-staging.yml     |                         |  deploy-production.yml   |
                 | (Deploy to Hostinger VPS)|                         | (Deploy to Hostinger VPS)|
                 +--------------------------+                         +--------------------------+
                              |                                                     |
                    SSH -> /opt/bazaarhub/staging/                        SSH -> /opt/bazaarhub/production/
```

### Workflow Specifications

| Workflow File | Trigger Conditions | Permissions | Artifacts / Outputs | Primary Target |
| :--- | :--- | :--- | :--- | :--- |
| [`.github/workflows/backend-ci.yml`](file:///Users/ajaykumar/bazaarhub-backend/.github/workflows/backend-ci.yml) | - PR targeting `main`<br>- Push to `main`<br>- `workflow_dispatch` | `contents: read` | Surefire XML test reports (`target/surefire-reports`) | Ephemeral GitHub Runner |
| [`.github/workflows/build-and-push.yml`](file:///Users/ajaykumar/bazaarhub-backend/.github/workflows/build-and-push.yml) | - Successful verify job<br>- Push to `main`<br>- Release tag `v*`<br>- `workflow_dispatch` | `contents: read`<br>`packages: write` | Immutable Docker image in GHCR (`sha-<commit>`, `<semver>`, `digest`) | GitHub Container Registry (`ghcr.io`) |
| [`.github/workflows/deploy-staging.yml`](file:///Users/ajaykumar/bazaarhub-backend/.github/workflows/deploy-staging.yml) | - `workflow_run` on `build-and-push` (branch `main`)<br>- `workflow_dispatch` | `contents: read` | Deployed container, health status report | Hostinger VPS Staging (`/opt/bazaarhub/staging/`) |
| [`.github/workflows/deploy-production.yml`](file:///Users/ajaykumar/bazaarhub-backend/.github/workflows/deploy-production.yml) | - Git tag matching `v*`<br>- `workflow_dispatch` with release tag | `contents: read` | Deployed release container, rollback state | Hostinger VPS Production (`/opt/bazaarhub/production/`) |

---

## 3. GitHub Container Registry (GHCR) Authentication
Images are hosted on GHCR under the repository namespace:
`ghcr.io/<github-owner>/bazaarhub-backend`

### Authentication in GitHub Actions
In workflow runners, authentication uses the ephemeral `GITHUB_TOKEN`:
```yaml
- name: Log in to GitHub Container Registry
  uses: docker/login-action@v3
  with:
    registry: ghcr.io
    username: ${{ github.actor }}
    password: ${{ secrets.GITHUB_TOKEN }}
```

### Authentication on Hostinger VPS
The VPS pulls private images using a GitHub Personal Access Token (PAT) with `read:packages` scope, stored in GitHub repository secrets as `GHCR_PULL_TOKEN`:
```bash
echo "${GHCR_TOKEN}" | docker login ghcr.io -u "${GHCR_USER}" --password-stdin
```
*Note: Tokens are passed via standard input (`--password-stdin`) to prevent credentials from being exposed in system process tables (`ps aux`) or shell history.*

---

## 4. Immutable Tagging & Image Promotion
We enforce immutable image tagging:
1. Every commit build generates:
   - `sha-<7-character-short-sha>` (e.g. `sha-35f1f10`)
   - `<full-40-character-sha>`
   - `staging` (moving pointer used strictly for local debugging)
2. Every release build generates:
   - `<semver-tag>` (e.g. `v1.0.0`)
   - `latest`
3. Deployments reference exact immutable tags or SHA256 digests (`@sha256:...`) to ensure byte-level reproducibility.

---

## 5. Security & Isolation Controls
- **Ephemeral Credentials**: Temporary session credentials are used where possible.
- **SSH Key Pair Authentication**: SSH password authentication is strictly disabled on the VPS; automated deployments authenticate via dedicated ED25519 or RSA 4096-bit private keys.
- **Non-Root Execution**: Container runtime runs under non-root UID `10001` (`bazaarhub`).
- **No Git Secrets**: Environment variables (`.env`) reside strictly on the server filesystem.
