#!/bin/bash
set -euo pipefail

# deploy-app.sh
# Usage: sudo /opt/apnafund/deploy-app.sh [env]
# Default env = dev
#
# Requirements:
# - aws CLI v2 present on the instance (or available in PATH)
# - instance role or credentials with read access to:
#   s3://apnafund-app-861082243595-ap-south-1/<env>/app/
# - deploy-nginx.sh present at /opt/apnafund/deploy-nginx.sh (optional; will be invoked if config exists)
#
# Behavior additions:
# - reads DB secret from Secrets Manager: secret id "apnafund/${ENV}/postgres"
# - injects DB_PASSWORD into downloaded env file (env-<env>.properties)
# - ensures jq installed by app-setup.sh has already provided parsing tools

APNA_ENV="${1:-dev}"
AWS_REGION="${AWS_REGION:-ap-south-1}"                # can be overridden in env
APP_BUCKET="apnafund-app-861082243595-${AWS_REGION}"
CONFIG_BUCKET="apnafund-config-861082243595-${AWS_REGION}"
BASE_PREFIX="apnafund/${APNA_ENV}"
APP_PREFIX="${BASE_PREFIX}/app"
JAR_KEY="${APP_PREFIX}/apnafund-server.jar"
ENV_KEY="${APP_PREFIX}/env-${APNA_ENV}.properties"
SERVICE_KEY="${APP_PREFIX}/apnafund.service"
NGINX_CONFIG_KEY="${BASE_PREFIX}/config/nginx/apnafund.conf"   # config bucket path (site-level)
SECRETS_SECRET_ID="${BASE_PREFIX}/postgres"    # SecretsManager secret id
SSM_SNS_USER_EVENTS_ARN="/apnafund/${APNA_ENV}/sns/user-events-arn"
SSM_SNS_SUPPORT_EVENTS_ARN="/apnafund/${APNA_ENV}/sns/support-events-arn"

APP_DIR="/opt/apnafund"
APP_USER="apnafund"
JAR_PATH="${APP_DIR}/app/apnafund-server.jar"
ENV_PATH="${APP_DIR}/config/env-${APNA_ENV}.properties"
SERVICE_PATH="/etc/systemd/system/apnafund.service"
RELEASES_DIR="${APP_DIR}/releases"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"

log(){ echo "==> $*"; }
err(){ echo "ERROR: $*" >&2; exit 1; }

if [ "$(id -u)" -ne 0 ]; then
  err "Run as root: sudo $0"
fi

if ! command -v aws >/dev/null 2>&1; then
  err "aws CLI not found. Install AWS CLI v2 or ensure it's in PATH."
fi

chown -R "$APP_USER":"$APP_USER" "$APP_DIR/app"
chown -R "$APP_USER":"$APP_USER" "$APP_DIR/config"
chown -R "$APP_USER":"$APP_USER" "$APP_DIR/releases"

# ---- Helper: safely replace or insert KEY=VALUE in env file ----
# This handles special characters safely.
upsert_env_key() {
  local file="$1" key="$2" val="$3"
  local tmp="${file}.tmp.$$"
  local escaped_val
  # write to tmp, replacing the key if present, else append
  # Use awk for predictable behavior
  awk -v k="$key" -v v="$val" '
    BEGIN { found=0 }
    /^#/ { print; next }            # preserve comments
    $0 ~ "^[[:space:]]*"k"=" {
      print k "=" v
      found=1
      next
    }
    { print }
    END { if (found==0) print k "=" v }
  ' "$file" > "$tmp" 2>/dev/null || {
    # If file doesn't exist, create it
    echo "${key}=${val}" > "$tmp"
  }
  mv "$tmp" "$file"
  chmod 600 "$file"
  chown "$APP_USER":"$APP_USER" "$file" || true
}

get_ssm_param() {
  aws ssm get-parameter \
    --name "$1" \
    --region "$AWS_REGION" \
    --query "Parameter.Value" \
    --output text 2>/dev/null || true
}

# 1) Download jar from S3 (backup previous)
log "Checking for jar s3://$APP_BUCKET/$JAR_KEY"
if aws s3 ls "s3://$APP_BUCKET/$JAR_KEY" >/dev/null 2>&1; then
  if [ -f "$JAR_PATH" ]; then
    BACKUP_JAR="${RELEASES_DIR}/apnafund-server-${TIMESTAMP}.jar"
    log "Backing up existing jar to $BACKUP_JAR"
    mv "$JAR_PATH" "$BACKUP_JAR"
    chown "$APP_USER":"$APP_USER" "$BACKUP_JAR" || true
  fi

  log "Downloading jar..."
  aws s3 cp "s3://$APP_BUCKET/$JAR_KEY" "$JAR_PATH" --only-show-errors
  chown "$APP_USER":"$APP_USER" "$JAR_PATH" || true
  chmod 640 "$JAR_PATH" || true
else
  log "No jar found at s3://$APP_BUCKET/$JAR_KEY -- leaving existing jar in place (if any)."
fi

# 2) Download env file
log "Checking for env file s3://$APP_BUCKET/$ENV_KEY"
if aws s3 ls "s3://$APP_BUCKET/$ENV_KEY" >/dev/null 2>&1; then
  log "Downloading env file..."
  aws s3 cp "s3://$APP_BUCKET/$ENV_KEY" "$ENV_PATH" --only-show-errors
  chown "$APP_USER":"$APP_USER" "$ENV_PATH" || true
  chmod 600 "$ENV_PATH" || true
else
  log "No env file found at s3://$APP_BUCKET/$ENV_KEY -- not changing env."
fi

# 2b) Inject DB password from Secrets Manager (Option B)
# Only if the secretsmanager secret exists and we have jq
if command -v jq >/dev/null 2>&1; then
  log "Attempting to read DB secret from Secrets Manager: ${SECRETS_SECRET_ID}"
  set +e
  secret_json=$(aws secretsmanager get-secret-value --secret-id "${SECRETS_SECRET_ID}" --region "${AWS_REGION}" --query SecretString --output text 2>/dev/null)
  rc=$?
  set -e
  if [ $rc -eq 0 ] && [ -n "$secret_json" ]; then
    # parse password (adjust key if your secret uses a different field)
    DB_PASSWORD_VAL=$(printf '%s' "$secret_json" | jq -r '
      .db_password
      // .password
      // .pass
      // .app_pass
      // .deploy_pass
      // empty
    ')
    if [ -z "$DB_PASSWORD_VAL" ] || [ "$DB_PASSWORD_VAL" = "null" ]; then
      log "WARNING: couldn't find 'password' field in secret JSON. Skipping injection."
    else
      log "Injecting DB_PASSWORD into env file ($ENV_PATH)"
      # ensure env file exists
      if [ ! -f "$ENV_PATH" ]; then
        echo "DB_PASSWORD=${DB_PASSWORD_VAL}" > "$ENV_PATH"
        chown "$APP_USER":"$APP_USER" "$ENV_PATH" || true
        chmod 600 "$ENV_PATH"
      else
        # Upsert DB_PASSWORD key (atomic)
        upsert_env_key "$ENV_PATH" "DB_PASSWORD" "$DB_PASSWORD_VAL"
      fi
    fi
  else
    log "No secret available at Secrets Manager (${SECRETS_SECRET_ID}) or permission denied. Skipping DB injection."
  fi
else
  log "jq not installed; cannot parse Secrets Manager JSON. Skipping DB injection."
fi

log "Fetching SNS ARNs from SSM Parameter Store"

SNS_USER_EVENTS_ARN_VAL="$(get_ssm_param "$SSM_SNS_USER_EVENTS_ARN")"
SNS_SUPPORT_EVENTS_ARN_VAL="$(get_ssm_param "$SSM_SNS_SUPPORT_EVENTS_ARN")"

if [ -n "$SNS_USER_EVENTS_ARN_VAL" ]; then
  upsert_env_key "$ENV_PATH" "USER_EVENTS_TOPIC_ARN" "$SNS_USER_EVENTS_ARN_VAL"
else
  log "WARNING: USER_EVENTS_TOPIC_ARN not found in SSM"
fi

if [ -n "$SNS_SUPPORT_EVENTS_ARN_VAL" ]; then
  upsert_env_key "$ENV_PATH" "SUPPORT_EVENTS_TOPIC_ARN" "$SNS_SUPPORT_EVENTS_ARN_VAL"
else
  log "WARNING: SUPPORT_EVENTS_TOPIC_ARN not found in SSM"
fi

# 3) Download systemd unit if present
SERVICE_UPDATED=0
log "Checking for service unit s3://$APP_BUCKET/$SERVICE_KEY"
if aws s3 ls "s3://$APP_BUCKET/$SERVICE_KEY" >/dev/null 2>&1; then
  log "Downloading apnafund.service from S3..."
  TMP_SERVICE="/tmp/apnafund.service.${TIMESTAMP}"
  aws s3 cp "s3://$APP_BUCKET/$SERVICE_KEY" "$TMP_SERVICE" --only-show-errors

  # If different from current, backup & replace
  if [ -f "$SERVICE_PATH" ]; then
    if ! cmp -s "$TMP_SERVICE" "$SERVICE_PATH"; then
      BACKUP_SERVICE="/etc/systemd/system/apnafund.service.${TIMESTAMP}.bak"
      log "Backing up existing service unit to $BACKUP_SERVICE"
      cp "$SERVICE_PATH" "$BACKUP_SERVICE"
      log "Installing new service unit"
      cp "$TMP_SERVICE" "$SERVICE_PATH"
      chmod 644 "$SERVICE_PATH"
      SERVICE_UPDATED=1
    else
      log "Downloaded service unit is identical to current; no change."
    fi
  else
    log "No existing service unit; installing new one"
    cp "$TMP_SERVICE" "$SERVICE_PATH"
    chmod 644 "$SERVICE_PATH"
    SERVICE_UPDATED=1
  fi

  rm -f "$TMP_SERVICE" || true
else
  log "No service unit present in S3 at $SERVICE_KEY; skipping service update."
fi

# If infra updated, reload systemd
if [ "$SERVICE_UPDATED" -eq 1 ]; then
  log "Reloading systemd daemon due to service unit change"
  systemctl daemon-reload
fi

# 4) Deploy nginx config from config bucket (if provided) by invoking deploy-nginx.sh
if [ -x "/opt/apnafund/bin/deploy-nginx.sh" ]; then
  log "Checking if nginx config exists in config bucket (s3://$CONFIG_BUCKET/$NGINX_CONFIG_KEY)"
  if aws s3 ls "s3://$CONFIG_BUCKET/$NGINX_CONFIG_KEY" >/dev/null 2>&1; then
    log "Invoking /opt/apnafund/bin/deploy-nginx.sh $APNA_ENV to apply nginx config"
    /opt/apnafund/bin/deploy-nginx.sh "$APNA_ENV"
  else
    log "No nginx config found in config bucket for env=$APNA_ENV"
  fi
else
  log "/opt/apnafund/bin/deploy-nginx.sh not present or not executable; skipping nginx deploy"
fi

# 5) Start/restart the apnafund infra if jar exists
if [ -f "$JAR_PATH" ]; then
  log "Enabling apnafund service"
  systemctl enable apnafund || true

  log "Restarting apnafund service"
  if systemctl restart apnafund; then
    log "Service restarted successfully"
  else
    log "Service restart failed — attempting status and journal for last 200 lines"
    systemctl status apnafund --no-pager || true
    journalctl -u apnafund -n 200 --no-pager || true
    err "apnafund service restart failed"
  fi

  sleep 2
  systemctl status apnafund --no-pager
else
  log "JAR not present at $JAR_PATH; cannot start service. Manual action required."
fi

log "Deploy script finished successfully."
exit 0
