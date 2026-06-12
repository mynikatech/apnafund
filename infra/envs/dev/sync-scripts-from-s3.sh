#!/bin/bash
set -euo pipefail

LOG="[sync-s3]"
APNA_ENV="${APNA_ENV:-dev}"
AWS_REGION="${AWS_REGION:-ap-south-1}"
S3_CONFIG_BUCKET="${S3_CONFIG_BUCKET:-apnafund-config-861082243595-ap-south-1}"

# S3 prefix: apnafund/dev/
PREFIX="apnafund/${APNA_ENV}"

BASE_DIR="/opt/apnafund"
BIN_DIR="${BASE_DIR}/bin"
LOG_DIR="/var/log/apnafund"

mkdir -p "$BIN_DIR" "$LOG_DIR"

echo "$LOG Sync started for env=$APNA_ENV bucket=$S3_CONFIG_BUCKET"

############################################
# LIST OF FILES TO DOWNLOAD
############################################
FILES=(
  "sync-scripts-from-s3.sh"
  "startup.sh"
  "shutdown.sh"
  "ssm-refresh.sh"
  "apnafund-disk-setup.sh"
  "nginx-install.sh"
  "nginx-ssl-setup.sh"
  "apnafund-startup.service"
  "apnafund.service"
  "apnafund-env.conf"
  "postgres-install.sh"
  "postgres-backup.sh"
  "install-postgres-backup-cron.sh"
  "cron-install.sh"
)

############################################
# DOWNLOAD EACH FILE
############################################
for FILE in "${FILES[@]}"; do
  SRC="s3://${S3_CONFIG_BUCKET}/${PREFIX}/${FILE}"

  if aws s3api head-object --bucket "$S3_CONFIG_BUCKET" --key "${PREFIX}/${FILE}" --region "$AWS_REGION" >/dev/null 2>&1; then
    echo "$LOG Downloading $FILE"
    aws s3 cp "$SRC" "${BIN_DIR}/${FILE}" --region "$AWS_REGION" --quiet || {
      echo "$LOG ERROR downloading $FILE"
      continue
    }
  else
    echo "$LOG WARNING: File not found in S3: $FILE"
    continue
  fi

  # Permissions
  case "$FILE" in
    *.service)
      # service files go to systemd, not /opt/apnafund/bin
      mv "${BIN_DIR}/${FILE}" "/etc/systemd/system/${FILE}"
      chmod 644 "/etc/systemd/system/${FILE}"
      chown root:root "/etc/systemd/system/${FILE}"
      ;;
    *.sh)
      chmod +x "${BIN_DIR}/${FILE}"
      chown root:root "${BIN_DIR}/${FILE}"
      ;;
    *)
      chmod 644 "${BIN_DIR}/${FILE}"
      chown root:root "${BIN_DIR}/${FILE}"
      ;;
  esac
done

############################################
# Reload systemd if any .service updated
############################################
systemctl daemon-reload || true

dos2unix /opt/apnafund/bin/*.sh 2>/dev/null || true

echo "$LOG Sync complete."
