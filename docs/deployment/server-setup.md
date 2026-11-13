# Hostinger VPS Server Setup, Hardening & Reverse Proxy Guide

## 1. Overview
This guide provides complete instructions for provisioning, hardening, and configuring a Hostinger Linux VPS (Ubuntu 22.04 / 24.04 LTS) to host the BazaarHub Backend Modular Monolith for Staging and Production.

---

## 2. Infrastructure Architecture & Sizing

### Minimum Hostinger VPS Specifications
- **Recommended Plan**: Hostinger KVM 2 or KVM 4 VPS
- **CPU**: 2–4 vCPU cores
- **RAM**: 4 GB (minimum for single-host Staging + Production) or 8 GB (recommended)
- **Disk**: 50 GB+ NVMe SSD
- **OS**: Ubuntu 22.04 LTS or 24.04 LTS (64-bit)

### Single VPS vs. Separate VPS Trade-Offs

| Evaluation Metric | Single VPS (Staging + Production) | Separate VPS per Environment |
| :--- | :--- | :--- |
| **Cost** | Minimal (1 VPS bill) | 2x Infrastructure cost |
| **Resource Contention** | Heavy staging tests (e.g. load test) can starve production CPU/RAM | Complete physical CPU/RAM isolation |
| **Security Isolation** | Shared root/docker daemon; shared kernel | Total network and credential isolation |
| **Blast Radius** | VPS crash or OOM panic takes down both environments | Staging failures cannot affect production |
| **Recommendation** | Acceptable for early MVP / pre-launch validation | **Mandatory** once customer transactions and real payments begin |

> **Architectural Decision**: Our directory structure (`/opt/bazaarhub/staging/` and `/opt/bazaarhub/production/`) and port isolation (`8081` vs `8080`) are designed so that both environments run safely on one VPS initially, but can be split across two separate VPS instances without modifying any workflow or compose file!

---

## 3. Hostinger Server Prerequisites & Initial Setup

### Step 1: System Update & Package Installation
```bash
sudo apt-get update && sudo apt-get upgrade -y
sudo apt-get install -y curl wget git ufw ca-certificates gnupg lsb-release
```

### Step 2: Install Docker Engine & Docker Compose Plugin
```bash
# Add Docker's official GPG key:
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# Add Docker repository to Apt sources:
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

Verify Docker installation:
```bash
docker --version
docker compose version
```

### Step 3: Create Dedicated Deployer User
Do not run CI/CD deployments as root.
```bash
# Create deployer user
sudo adduser --disabled-password --gecos "" deployer

# Add to docker group
sudo usermod -aG docker deployer

# Set up SSH directory for deployer
sudo mkdir -p /home/deployer/.ssh
sudo chmod 700 /home/deployer/.ssh

# Authorize your GitHub Actions deployment public key
sudo touch /home/deployer/.ssh/authorized_keys
sudo chmod 600 /home/deployer/.ssh/authorized_keys
sudo chown -R deployer:deployer /home/deployer/.ssh
```

### Step 4: Create Server Directory Convention
```bash
sudo mkdir -p /opt/bazaarhub/staging
sudo mkdir -p /opt/bazaarhub/production

# Grant ownership to deployer user
sudo chown -R deployer:deployer /opt/bazaarhub
sudo chmod -R 775 /opt/bazaarhub
```

---

## 4. Firewall (UFW) Configuration
Only SSH, HTTP, and HTTPS should be exposed to the public internet. The Spring Boot application ports (`8080`, `8081`) must be inaccessible from external networks.

```bash
# Reset UFW to default deny incoming
sudo ufw default deny incoming
sudo ufw default allow outgoing

# Allow standard SSH (or custom port if modified)
sudo ufw allow 22/tcp

# Allow HTTP and HTTPS for Reverse Proxy
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Enable Firewall
sudo ufw enable
sudo ufw status verbose
```

*Note: In `docker-compose.staging.yml` and `docker-compose.production.yml`, ports are explicitly bound to `127.0.0.1:${PORT}:8080`. This prevents Docker from bypassing UFW iptables rules.*

---

## 5. DNS Configuration
In your domain registrar (Hostinger, Cloudflare, Route53), configure DNS `A` records pointing to your Hostinger VPS public IP address:

| Type | Name / Host | Target Value | Purpose |
| :---: | :--- | :--- | :--- |
| `A` | `api.<your-domain>` | `<Hostinger-VPS-IP>` | Production API endpoint |
| `A` | `staging-api.<your-domain>` | `<Hostinger-VPS-IP>` | Staging API endpoint |

---

## 6. Reverse Proxy & SSL Configuration

Choose either **Nginx** or **Caddy**.

---

### Option A: Nginx + Let's Encrypt Certbot (Standard Enterprise)

#### 1. Install Nginx and Certbot
```bash
sudo apt-get install -y nginx certbot python3-certbot-nginx
```

#### 2. Create Production Nginx Configuration
File: `/etc/nginx/sites-available/bazaarhub-production`
```nginx
server {
    server_name api.<your-domain>;

    # Security Headers
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "no-referrer-when-downgrade" always;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Timeouts for API calls
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}
```

#### 3. Create Staging Nginx Configuration
File: `/etc/nginx/sites-available/bazaarhub-staging`
```nginx
server {
    server_name staging-api.<your-domain>;

    location / {
        proxy_pass http://127.0.0.1:8081;
        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}
```

#### 4. Enable Sites & Request SSL Certificates
```bash
sudo ln -s /etc/nginx/sites-available/bazaarhub-production /etc/nginx/sites-enabled/
sudo ln -s /etc/nginx/sites-available/bazaarhub-staging /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx

# Obtain automatic SSL certificates via Certbot:
sudo certbot --nginx -d api.<your-domain>
sudo certbot --nginx -d staging-api.<your-domain>
```

---

### Option B: Caddy (Automatic HTTPS Alternative)
Caddy automatically manages and renews Let's Encrypt TLS certificates with zero manual intervention.

File: `/etc/caddy/Caddyfile`
```caddy
api.<your-domain> {
    reverse_proxy 127.0.0.1:8080
    header {
        Strict-Transport-Security "max-age=31536000; includeSubDomains"
        X-Content-Type-Options "nosniff"
        X-Frame-Options "DENY"
    }
}

staging-api.<your-domain> {
    reverse_proxy 127.0.0.1:8081
    header {
        X-Content-Type-Options "nosniff"
        X-Frame-Options "DENY"
    }
}
```
Reload Caddy:
```bash
sudo systemctl reload caddy
```

---

## 7. Operational Troubleshooting Cheatsheet

### Check Running Containers
```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

### Inspect Container Logs
```bash
# Staging logs
docker logs -f --tail=100 bazaarhub-staging-backend

# Production logs
docker logs -f --tail=100 bazaarhub-production-backend
```

### Test Local Health Endpoints
```bash
# Staging (Port 8081)
curl -i http://127.0.0.1:8081/actuator/health

# Production (Port 8080)
curl -i http://127.0.0.1:8080/actuator/health
```

### Restart a Specific Environment
```bash
cd /opt/bazaarhub/staging && docker compose -f docker-compose.staging.yml restart
cd /opt/bazaarhub/production && docker compose -f docker-compose.production.yml restart
```
