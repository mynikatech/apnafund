#!/bin/bash
set -euo pipefail

# generate-cert.sh [env] [domain]
# Example: sudo /opt/apnafund/bin/generate-cert.sh dev api-dev.apnafund.mynikatech.in
ENV="${1:-dev}"
DOMAIN="${2:-api-dev.apnafund.mynikatech.in}"
LE_DIR="/etc/letsencrypt/live/${DOMAIN}"
LOG="/var/log/apnafund/generate-cert.log"

log(){ echo "==> $*"; echo "[$(date -u +%Y-%m-%dT%H:%M:%SZ)] $*" >> "$LOG"; }

if [ "$(id -u)" -ne 0 ]; then
  echo "Run as root"; exit 1
fi

# Install certbot + route53 plugin (idempotent)
if ! command -v certbot >/dev/null 2>&1; then
  log "Installing certbot + dns-route53 plugin"
  yum install -y epel-release || true
  yum install -y python3 python3-pip || true
  pip3 install --upgrade certbot certbot-dns-route53 || true
fi

# Ensure AWS IAM permissions (Route53) are available via instance role or env creds
log "Requesting certificate for $DOMAIN using DNS (Route53 plugin)"

# Run certbot (non-interactive). Will create TXT record using instance role if allowed.
certbot certonly \
  --agree-tos \
  --non-interactive \
  --dns-route53 \
  -m "support@mynikatech.in" \
  -d "$DOMAIN"

# Validate files exist
if [ ! -f "${LE_DIR}/fullchain.pem" ] || [ ! -f "${LE_DIR}/privkey.pem" ]; then
  log "ERROR: certificate files missing in ${LE_DIR}"
  exit 2
fi

log "Certificate created at ${LE_DIR}"

log "Certificate installation completed. Verify with: openssl s_client -connect ${DOMAIN}:443 -servername ${DOMAIN}"
