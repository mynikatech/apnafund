#!/bin/bash
set -euo pipefail

LOG="[nginx-ssl]"

SSL_DIR="/etc/nginx/ssl"

echo "$LOG Configuring SSL..."

#########################################
# Create SSL directory
#########################################
mkdir -p "$SSL_DIR"

#########################################
# Copy SSL cert and key if present
#########################################
if [ -f /opt/apnafund/config/server.crt ] && [ -f /opt/apnafund/config/server.key ]; then
    echo "$LOG Installing SSL certificate..."
    cp /opt/apnafund/config/server.crt "$SSL_DIR/server.crt"
    cp /opt/apnafund/config/server.key "$SSL_DIR/server.key"
    chmod 600 "$SSL_DIR/server.key"
else
    echo "$LOG WARNING: SSL cert/key not found in /opt/apnafund/config"
fi

#########################################
# Reload nginx only if config exists
#########################################
if systemctl is-active --quiet nginx; then
    echo "$LOG Reloading nginx..."
    systemctl reload nginx || true
else
    echo "$LOG NGINX not running; skipping reload."
fi

echo "$LOG nginx-ssl-setup completed."
