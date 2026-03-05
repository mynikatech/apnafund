#!/bin/bash
set -euo pipefail

LOG="[user-data]"

#############################################
# BASIC CONFIG
#############################################
APNA_ENV="dev"
AWS_REGION="${AWS_REGION:-ap-south-1}"
S3_CONFIG_BUCKET="${S3_CONFIG_BUCKET:-apnafund-config-861082243595-ap-south-1}"
S3_PREFIX="apnafund/${APNA_ENV}"

echo "$LOG Starting bootstrap for $APNA_ENV..."

#############################################
# REFRESH PACKAGE METADATA
#############################################
echo "$LOG Updating package metadata..."
dnf -y clean all || true
dnf -y makecache || true

#############################################
# INSTALL MINIMAL CORE PACKAGES
#############################################
echo "$LOG Installing core tools (docker, awscli)..."
dnf -y install docker awscli git jq unzip tar wget bind-utils which || true

#############################################
# ENABLE DOCKER
#############################################
systemctl enable --now docker || true
groupadd -f docker || true
usermod -aG docker ec2-user || true

#############################################
# CREATE REQUIRED FOLDERS
#############################################
echo "$LOG Creating ApnaFund directory structure..."
install -d -m 755 /opt/apnafund
install -d -m 755 /opt/apnafund/bin
install -d -m 750 /opt/apnafund/postgres
install -d -m 750 /var/log/apnafund

chown -R ec2-user:ec2-user /opt/apnafund
chown -R ec2-user:ec2-user /opt/apnafund/bin

chown -R postgres:postgres /opt/apnafund/postgres

#############################################
# DOWNLOAD ONLY THE BOOTSTRAP SCRIPT
# This script will fetch ALL other scripts later
#############################################
BOOTSTRAP="/opt/apnafund/bin/sync-scripts-from-s3.sh"

echo "$LOG Downloading initial script sync-scripts-from-s3.sh..."
aws s3 cp \
  "s3://${S3_CONFIG_BUCKET}/${S3_PREFIX}/sync-scripts-from-s3.sh" \
  "$BOOTSTRAP" \
  --region "$AWS_REGION" || true

chmod +x "$BOOTSTRAP" || true

#############################################
# RUN INITIAL SYNC (fetch latest scripts)
#############################################
echo "$LOG Running initial sync from S3..."
if [ -x "$BOOTSTRAP" ]; then
  "$BOOTSTRAP" || true
else
  echo "$LOG ERROR: sync-scripts-from-s3.sh missing!" >&2
fi

#############################################
# ENABLE STARTUP SERVICE
#############################################
systemctl daemon-reload || true
systemctl enable apnafund-startup.service || true

echo "$LOG Bootstrap complete."
