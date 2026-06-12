#!/bin/bash
set -euo pipefail

LOG_PREFIX="[postgres-backup]"

APNA_ENV="${APNA_ENV:-dev}"
AWS_REGION="${AWS_REGION:-ap-south-1}"
S3_BACKUP_BUCKET="${S3_BACKUP_BUCKET:-apnafund-app-861082243595-ap-south-1}"

DB_NAME="apnafund"

BACKUP_DIR="/opt/apnafund/backups/postgres"

mkdir -p "$BACKUP_DIR"

chown postgres:postgres "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

DATE=$(date +%F_%H-%M-%S)

FILE="${BACKUP_DIR}/${DB_NAME}_${DATE}.dump"

echo "$LOG_PREFIX Starting backup"

pg_dump \
  -U postgres \
  -Fc \
  --clean \
  --if-exists \
  "$DB_NAME" \
  -f "$FILE"

gzip "$FILE"

echo "$LOG_PREFIX Uploading to S3"

aws s3 cp \
  "${FILE}.gz" \
  "s3://${S3_BACKUP_BUCKET}/backups/${APNA_ENV}/postgres/" \
  --region "$AWS_REGION"

find "$BACKUP_DIR" -type f -mtime +7 -delete

echo "$LOG_PREFIX Backup complete"