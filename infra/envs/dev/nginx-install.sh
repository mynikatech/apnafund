#!/bin/bash
set -euo pipefail

LOG="[nginx-install]"

echo "$LOG Installing NGINX..."

#########################################
# Install nginx (idempotent)
#########################################
dnf -y install nginx || {
    echo "$LOG ERROR: Failed to install nginx."
    exit 1
}

#########################################
# Create config directory
#########################################
mkdir -p /etc/nginx/conf.d
mkdir -p /var/log/nginx

#########################################
# Copy environment config (if available)
#########################################
if [ -f /opt/apnafund/config/apnafund-env.conf ]; then
    echo "$LOG Installing apnafund-env.conf"
    cp /opt/apnafund/config/apnafund-env.conf /etc/nginx/conf.d/apnafund-env.conf
else
    echo "$LOG WARNING: apnafund-env.conf not found in /opt/apnafund/config"
fi

#########################################
# Enable and start nginx
#########################################
systemctl enable nginx || true
systemctl restart nginx || true

echo "$LOG nginx-install completed."
