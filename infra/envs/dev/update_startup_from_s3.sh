#!/bin/bash
set -euo pipefail

# -------------------------------
# Environment Variables
# -------------------------------
BUCKET="${S3_CONFIG_BUCKET:-apnafund-config-861082243595-ap-south-1}"
REGION="${AWS_REGION:-ap-south-1}"
ENV="${ENV:-dev}"

BASE_PATH="apnafund/${ENV}"

# -------------------------------
# Destination Paths
# -------------------------------
DST_DIR="/opt/apnafund/bin"
SYSTEMD_DIR="/etc/systemd/system"

DST_STARTUP="${DST_DIR}/startup.sh"
DST_DISK="/usr/local/bin/apnafund-disk-setup.sh"
DST_SERVICE="${SYSTEMD_DIR}/apnafund-startup.service"
DST_APP_SERVICE="${SYSTEMD_DIR}/apnafund.service"
DST_ENV_CONF="/opt/apnafund/apnafund-env.conf"
DST_NGINX_INSTALL="${DST_DIR}/nginx-install.sh"
DST_NGINX_SSL="${DST_DIR}/nginx-ssl-setup.sh"
DST_SSM_REFRESH="${DST_DIR}/ssm-refresh.sh"

ETAG_FILE="/opt/apnafund/.startup.etag"
LOG="/var/log/apnafund/startup.log"

mkdir -p "$DST_DIR"
touch "$LOG"

# -------------------------------
# Helper function
# -------------------------------
sync_from_s3() {
  local key="$1"
  local dst="$2"

  echo "Syncing s3://$BUCKET/$key → $dst"
  aws s3 cp "s3://$BUCKET/$key" "$dst" --region "$REGION" --quiet

  # Set execute bit for scripts
  if [[ "$dst" == *.sh ]]; then
    chmod +x "$dst"
  fi

  chown root:root "$dst" || true
}

# -------------------------------
# startup.sh with ETag detection
# -------------------------------
STARTUP_KEY="${BASE_PATH}/startup.sh"

REMOTE_ETAG=$(aws s3api head-object \
    --bucket "$BUCKET" \
    --key "$STARTUP_KEY" \
    --query ETag \
    --output text \
    --region "$REGION" | tr -d '"')

LOCAL_ETAG=""
[[ -f "$ETAG_FILE" ]] && LOCAL_ETAG=$(cat "$ETAG_FILE" || true)

if [[ "$REMOTE_ETAG" != "$LOCAL_ETAG" ]] || [[ ! -s "$DST_STARTUP" ]]; then
  echo "[$(date)] Updating startup.sh (etag $REMOTE_ETAG)" | tee -a "$LOG"
  sync_from_s3 "$STARTUP_KEY" "$DST_STARTUP"
  echo -n "$REMOTE_ETAG" > "$ETAG_FILE"
  UPDATED_STARTUP=1
else
  echo "[$(date)] startup.sh unchanged (etag $REMOTE_ETAG)" | tee -a "$LOG"
  UPDATED_STARTUP=0
fi

# -------------------------------
# Remaining Files to Always Sync
# -------------------------------
FILES_TO_SYNC=(
  "apnafund-disk-setup.sh:$DST_DISK"
  "apnafund-startup.service:$DST_SERVICE"
  "apnafund.service:$DST_APP_SERVICE"
  "apnafund-env.conf:$DST_ENV_CONF"
  "nginx-install.sh:$DST_NGINX_INSTALL"
  "nginx-ssl-setup.sh:$DST_NGINX_SSL"
  "ssm-refresh.sh:$DST_SSM_REFRESH"
)

for pair in "${FILES_TO_SYNC[@]}"; do
  KEY="${pair%%:*}"
  DST="${pair##*:}"

  FULL_KEY="${BASE_PATH}/${KEY}"

  if aws s3api head-object --bucket "$BUCKET" --key "$FULL_KEY" --region "$REGION" >/dev/null 2>&1; then
    sync_from_s3 "$FULL_KEY" "$DST"
  else
    echo "Missing in S3: $FULL_KEY"
  fi
done

# -------------------------------
# Reload systemd
# -------------------------------
systemctl daemon-reload || true

# -------------------------------
# Restart main orchestrator if updated
# -------------------------------
if [[ "$UPDATED_STARTUP" -eq 1 ]]; then
  systemctl restart apnafund-startup.service || true
  echo "[$(date)] Restarted apnafund-startup.service" | tee -a "$LOG"
else
  echo "[$(date)] No restart needed" | tee -a "$LOG"
fi

echo "[$(date)] update_startup_from_s3.sh complete." | tee -a "$LOG"
