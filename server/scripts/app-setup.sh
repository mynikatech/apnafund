#!/bin/bash
set -euo pipefail

# app-setup.sh - prepares EC2 environment for ApnaFund server (production-ready)
# Run as root: sudo ./app-setup.sh

###########################
# Config
###########################
APP_DIR="/opt/apnafund"
APP_USER="apnafund"
LOG_DIR="/var/log/apnafund"
WWW_LE_DIR="/var/www/letsencrypt"
WWW_APP_LE_DIR="/var/www/apnafund"
JAVA_PKG="java-21-amazon-corretto-headless"

###########################
# Helpers
###########################
log() { echo "==> $*"; }
err() { echo "ERROR: $*" >&2; exit 1; }

###########################
# Root check
###########################
if [[ "$EUID" -ne 0 ]]; then
  err "Run as root: sudo ./app-setup.sh"
fi

log "Starting ApnaFund app setup"

###########################
# Create system user & dirs
###########################
if ! id "$APP_USER" >/dev/null 2>&1; then
  log "Creating system user: $APP_USER"
  useradd -r -s /sbin/nologin --home-dir "$APP_DIR" "$APP_USER"
else
  log "User $APP_USER already exists"
fi

log "Creating directories"
mkdir -p "$APP_DIR/app" "$APP_DIR/config" "$APP_DIR/releases" "$APP_DIR/tmp" "$LOG_DIR" "$WWW_LE_DIR" "$WWW_APP_LE_DIR"
chmod 750 "$APP_DIR"
chmod 700 "$APP_DIR/tmp"
chmod 750 "$LOG_DIR"
chmod 755 "$WWW_LE_DIR"
chmod 755 "$WWW_APP_LE_DIR"

# Ensure app subdirs owned properly
chown -R "$APP_USER":"$APP_USER" "$APP_DIR/app" "$APP_DIR/releases" "$APP_DIR/config"

###########################
# Install Java (Corretto 21)
###########################
if command -v java >/dev/null 2>&1 && java -version 2>&1 | grep -qi "corretto"; then
  log "Amazon Corretto already installed"
else
  log "Installing Amazon Corretto 21..."
  if yum list "$JAVA_PKG" >/dev/null 2>&1; then
    yum install -y "$JAVA_PKG"
  else
    tmp_rpm="/tmp/corretto-21.rpm"
    curl -fsSL -o "$tmp_rpm" "https://corretto.aws/downloads/latest/amazon-corretto-21-x64-linux-jdk.rpm"
    yum install -y "$tmp_rpm"
    rm -f "$tmp_rpm"
  fi
fi

# Set JAVA_HOME
if [[ -d "/usr/lib/jvm/java-21-amazon-corretto" ]]; then
  export JAVA_HOME="/usr/lib/jvm/java-21-amazon-corretto"
  log "JAVA_HOME set to $JAVA_HOME"
fi

###########################
# Install awscli v2 (if missing)
###########################
if ! command -v aws >/dev/null 2>&1; then
  log "Installing AWS CLI v2..."
  yum install -y unzip || true
  tmp_zip="/tmp/awscliv2.zip"
  curl -fsSL -o "$tmp_zip" "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip"
  unzip -o "$tmp_zip" -d /tmp
  /tmp/aws/install -i /usr/local/aws-cli -b /usr/local/bin || /tmp/aws/install
  rm -rf /tmp/aws "$tmp_zip"
else
  log "AWS CLI already installed"
fi

###########################
# Ensure dos2unix exists (helpful for scripts)
###########################
if ! command -v dos2unix >/dev/null 2>&1; then
  log "Installing dos2unix (optional helper)"
  yum install -y dos2unix || true
fi

###########################
# Create log files (touch & set ownership)
###########################
mkdir -p "$APP_DIR/logs" || true
touch "$APP_DIR/logs/app.log" "$APP_DIR/logs/app-error.log" || true
chown -R "$APP_USER":"$APP_USER" "$APP_DIR/logs"
chmod 750 "$APP_DIR/logs" || true
chmod 640 "$APP_DIR/logs/"*.log

###########################
# Systemd: enable service (if unit already present)
###########################
if [ -f "/etc/systemd/system/apnafund.service" ]; then
  log "Found existing systemd unit; enabling service"
  systemctl daemon-reload || true
  systemctl enable apnafund || true
else
  log "No apnafund.service installed yet. Upload apnafund.service to S3 and run deploy-app.sh to install it."
fi

# Install jq (if missing)
if ! command -v jq >/dev/null 2>&1; then
  log "Installing jq..."
  yum install -y jq || {
    # fallback: try download
    curl -fsSL -o /usr/local/bin/jq "https://github.com/stedolan/jq/releases/latest/download/jq-linux64"
    chmod +x /usr/local/bin/jq
  }
else
  log "jq already installed"
fi
###########################
# Done
###########################
log "ApnaFund environment setup complete."

