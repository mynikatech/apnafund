#!/bin/bash
set -euo pipefail

LOG="[sync-app]"
APNA_ENV="${APNA_ENV:-dev}"
AWS_REGION="${AWS_REGION:-ap-south-1}"

APP_BUCKET="${APP_BUCKET:-apnafund-app-861082243595-ap-south-1}"
CONFIG_BUCKET="${CONFIG_BUCKET:-apnafund-config-861082243595-ap-south-1}"

# S3 prefixes
BASE_PREFIX="apnafund/${APNA_ENV}"
APP_PREFIX="${BASE_PREFIX}/app"
SCRIPT_PREFIX="${BASE_PREFIX}/config"
NGINX_PREFIX="${BASE_PREFIX}/config/nginx"

BASE_DIR="/opt/apnafund"
BIN_DIR="${BASE_DIR}/bin"
CONF_DIR="${BASE_DIR}/config"
LOG_DIR="/var/log/apnafund"

mkdir -p "$BIN_DIR" "$CONF_DIR" "$LOG_DIR"

echo "$LOG Sync started for env=$APNA_ENV"
echo "$LOG App bucket=$APP_BUCKET"
echo "$LOG Config bucket=$CONFIG_BUCKET"

############################################
# DOWNLOADABLE SCRIPTS (excluding self)
############################################
SCRIPT_FILES=(
  "app-setup.sh"
  "deploy-app.sh"
  "deploy-nginx.sh"
  "generate-cert.sh"
)

############################################
# DOWNLOADABLE CONFIG FILES
############################################
CONFIG_FILES=(
  "apnafund.service"
)

############################################
# NGINX SITE CONFIG
############################################
NGINX_FILES=(
  "apnafund.conf"
)

############################################
# DOWNLOAD SCRIPTS → /opt/apnafund/bin
############################################
for FILE in "${SCRIPT_FILES[@]}"; do
  SRC="s3://${APP_BUCKET}/${SCRIPT_PREFIX}/${FILE}"

  echo "$LOG Checking script: $FILE"
  if aws s3api head-object --bucket "$APP_BUCKET" --key "${SCRIPT_PREFIX}/${FILE}" --region "$AWS_REGION" >/dev/null 2>&1; then
    echo "$LOG Downloading $FILE"
    aws s3 cp "$SRC" "${BIN_DIR}/${FILE}" --region "$AWS_REGION" --quiet
    chmod +x "${BIN_DIR}/${FILE}"
    chown root:root "${BIN_DIR}/${FILE}"
  else
    echo "$LOG WARNING: Script not found: $FILE"
  fi
done

############################################
# DOWNLOAD CONFIG FILES
############################################
for FILE in "${CONFIG_FILES[@]}"; do
  SRC="s3://${APP_BUCKET}/${APP_PREFIX}/${FILE}"

  echo "$LOG Checking config file: $FILE"
  if aws s3api head-object --bucket "$APP_BUCKET" --key "${APP_PREFIX}/${FILE}" --region "$AWS_REGION" >/dev/null 2>&1; then
    echo "$LOG Downloading $FILE"
    aws s3 cp "$SRC" "${CONF_DIR}/${FILE}" --region "$AWS_REGION" --quiet
    chmod 644 "${CONF_DIR}/${FILE}"
    chown root:root "${CONF_DIR}/${FILE}"

    # Move .service file into systemd
    if [[ "$FILE" == *.service ]]; then
      mv "${CONF_DIR}/${FILE}" "/etc/systemd/system/${FILE}"
      chmod 644 "/etc/systemd/system/${FILE}"
      chown root:root "/etc/systemd/system/${FILE}"
    fi
  else
    echo "$LOG WARNING: Config file not found: $FILE"
  fi
done

############################################
# DOWNLOAD NGINX SITE CONFIG → /etc/nginx/conf.d/
############################################
for FILE in "${NGINX_FILES[@]}"; do
  SRC="s3://${CONFIG_BUCKET}/${NGINX_PREFIX}/${FILE}"

  echo "$LOG Checking nginx file: $FILE"
  if aws s3api head-object --bucket "$CONFIG_BUCKET" --key "${NGINX_PREFIX}/${FILE}" --region "$AWS_REGION" >/dev/null 2>&1; then
    echo "$LOG Downloading $FILE → /etc/nginx/conf.d/"
    aws s3 cp "$SRC" "/etc/nginx/conf.d/${FILE}" --region "$AWS_REGION" --quiet
    chmod 644 "/etc/nginx/conf.d/${FILE}"
    chown root:root "/etc/nginx/conf.d/${FILE}"
  else
    echo "$LOG WARNING: Nginx config not found: $FILE"
  fi
done

############################################
# DOWNLOAD FIREBASE SERVICE ACCOUNT
############################################
FIREBASE_PREFIX="${BASE_PREFIX}/config/firebase"
FIREBASE_FILE="firebase-service-account.json"

SRC="s3://${CONFIG_BUCKET}/${FIREBASE_PREFIX}/${FIREBASE_FILE}"
DEST="${BASE_DIR}/config/${FIREBASE_FILE}"

echo "$LOG Checking Firebase credentials"

if aws s3api head-object \
    --bucket "$CONFIG_BUCKET" \
    --key "${FIREBASE_PREFIX}/${FIREBASE_FILE}" \
    --region "$AWS_REGION" >/dev/null 2>&1; then

  echo "$LOG Downloading Firebase service account"
  aws s3 cp "$SRC" "$DEST" --region "$AWS_REGION" --quiet

  chown apnafund:apnafund "$DEST"
  chmod 600 "$DEST"
else
  echo "$LOG WARNING: Firebase credentials not found in S3"
fi

############################################
# Reload system services
############################################
echo "$LOG Reloading systemd..."
systemctl daemon-reload || true

echo "$LOG Reloading nginx..."
systemctl reload nginx || true

dos2unix ${BIN_DIR}/*.sh 2>/dev/null || true

echo "$LOG Sync complete."
