#!/bin/bash
set -euo pipefail

# deploy-nginx.sh — safe + template-rendered NGINX deployer
# Usage:
#   sudo /opt/apnafund/bin/deploy-nginx.sh <env>
#
# Example:
#   sudo deploy-nginx.sh dev

############################
# Inputs
############################
ENV="${1:-dev}"
DOMAIN="api-${ENV}.apnafund.mynikatech.in"

CONFIG_BUCKET="apnafund-config-861082243595-ap-south-1"
S3_KEY="apnafund/${ENV}/config/nginx/apnafund.conf"

############################
# Paths
############################
TMP_DIR="/tmp/apnafund_nginx"
TMP_TEMPLATE="${TMP_DIR}/template.conf"
TMP_RENDERED="${TMP_DIR}/rendered.conf"

LIVE_PATH="/etc/nginx/conf.d/apnafund.conf"
DISABLED_PATH="/etc/nginx/conf.d/apnafund.conf.disabled"

BACKUP_DIR="/etc/nginx/conf.d/backup"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"

FULLCHAIN="/etc/letsencrypt/live/${DOMAIN}/fullchain.pem"
PRIVKEY="/etc/letsencrypt/live/${DOMAIN}/privkey.pem"

############################
# Helpers
############################
log(){ echo "==> $*"; }
err(){ echo "ERROR: $*" >&2; exit 1; }

############################
# Root check
############################
if [ "$(id -u)" -ne 0 ]; then
  err "Run as root: sudo $0 <env>"
fi

############################
# Prepare directories
############################
mkdir -p "$TMP_DIR" "$BACKUP_DIR"
chmod 700 "$TMP_DIR"

############################
# Download nginx template
############################
log "Downloading nginx template from S3"
aws s3 cp "s3://${CONFIG_BUCKET}/${S3_KEY}" "$TMP_TEMPLATE" --only-show-errors \
  || err "Failed to download nginx template from S3"

############################
# Render template
############################
log "Rendering nginx template"
sed \
  -e "s|{{DOMAIN}}|${DOMAIN}|g" \
  -e "s|{{FULLCHAIN}}|${FULLCHAIN}|g" \
  -e "s|{{PRIVKEY}}|${PRIVKEY}|g" \
  "$TMP_TEMPLATE" > "$TMP_RENDERED"

############################
# Temporarily disable live config
############################
if [ -f "$LIVE_PATH" ]; then
  log "Temporarily disabling live config for validation"
  mv "$LIVE_PATH" "$DISABLED_PATH"
fi

############################
# Cleanup handler
############################
cleanup() {
  rm -f "$TMP_TEMPLATE" "$TMP_RENDERED"
}
trap cleanup EXIT

############################
# Validate nginx config
############################
log "Validating nginx configuration (nginx -t)"
if ! nginx -t; then
  log "Validation failed — restoring previous config"
  if [ -f "$DISABLED_PATH" ]; then
    mv "$DISABLED_PATH" "$LIVE_PATH"
  fi
  err "nginx -t FAILED — configuration invalid"
fi

############################
# Backup old config (if existed)
############################
if [ -f "$DISABLED_PATH" ]; then
  BACKUP_PATH="${BACKUP_DIR}/apnafund.conf.${TIMESTAMP}"
  log "Backing up previous config → $BACKUP_PATH"
  cp -p "$DISABLED_PATH" "$BACKUP_PATH"
fi

############################
# Deploy new config
############################
log "Deploying rendered config → $LIVE_PATH"
mv "$TMP_RENDERED" "$LIVE_PATH"
chmod 644 "$LIVE_PATH"
chown root:root "$LIVE_PATH"

############################
# Remove disabled config
############################
rm -f "$DISABLED_PATH"

############################
# Reload nginx
############################
log "Reloading nginx"
if ! systemctl reload nginx; then
  log "Reload failed — restoring last backup"
  if [ -f "$BACKUP_PATH" ]; then
    cp -p "$BACKUP_PATH" "$LIVE_PATH"
    systemctl reload nginx \
      || err "Rollback failed — manual intervention required"
    err "New config invalid — rollback completed"
  else
    err "No backup available — manual intervention required"
  fi
fi

log "NGINX deployed successfully for domain: $DOMAIN"
exit 0
