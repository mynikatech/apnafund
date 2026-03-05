#!/bin/bash
set -euo pipefail

LOG="[disk-setup]"

# AL2023 NVMe device name for the 2nd EBS volume
DATA_DEV="/dev/nvme1n1"
DATA_DIR="/opt/apnafund/postgres"

echo "$LOG Starting disk setup..."

#############################################
# Detect EBS Volume
#############################################
if ! lsblk | grep -q "nvme1n1"; then
    echo "$LOG No extra data volume found. Skipping."
    exit 0
fi

echo "$LOG Volume detected: $DATA_DEV"

#############################################
# Create mount directory
#############################################
mkdir -p "$DATA_DIR"

#############################################
# Format disk ONLY if it has no filesystem
#############################################
if ! blkid "$DATA_DEV" >/dev/null 2>&1; then
    echo "$LOG Formatting $DATA_DEV with XFS filesystem..."
    mkfs.xfs -f "$DATA_DEV"
else
    echo "$LOG $DATA_DEV already has a filesystem. Skipping format."
fi

#############################################
# Ensure fstab entry (Idempotent)
#############################################
if ! grep -q "^${DATA_DEV} " /etc/fstab; then
    echo "$LOG Adding fstab entry for $DATA_DEV → $DATA_DIR"
    echo "${DATA_DEV} ${DATA_DIR} xfs defaults,nofail 0 2" >> /etc/fstab
else
    echo "$LOG fstab entry already exists."
fi

#############################################
# Mount the disk
#############################################
echo "$LOG Mounting all filesystems..."
mount -a || true

#############################################
# Fix directory ownership
#############################################
echo "$LOG Setting permissions..."
chown -R ec2-user:ec2-user /opt/apnafund

echo "$LOG Disk setup complete."
