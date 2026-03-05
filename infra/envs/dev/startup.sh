#!/bin/bash
set -euo pipefail

LOG="[startup]"
APNA_ENV="${APNA_ENV:-dev}"
BASE_DIR="/opt/apnafund"
PG_SERVICE="postgresql-17"
NGINX_SERVICE="nginx"

echo "$LOG Starting startup.sh for env=$APNA_ENV"

#############################################
# 1. Load environment variables (optional)
#############################################
if [ -f "${BASE_DIR}/.env" ]; then
    echo "$LOG Loading environment variables from .env"
    set -a
    source "${BASE_DIR}/.env"
    set +a
else
    echo "$LOG .env not found — continuing."
fi

#############################################
# 2. Ensure PostgreSQL service is enabled + started
#############################################
echo "$LOG Ensuring PostgreSQL service is enabled and running..."
sudo systemctl enable "${PG_SERVICE}" || true
sudo systemctl start "${PG_SERVICE}" || true

#############################################
# 3. Wait for PostgreSQL readiness
#############################################
echo "$LOG Waiting for PostgreSQL service..."
for i in {1..30}; do
    if sudo -u postgres pg_isready -U postgres >/dev/null 2>&1; then
        echo "$LOG PostgreSQL is ready."
        break
    fi
    sleep 1
done

if ! sudo -u postgres pg_isready -U postgres >/dev/null 2>&1; then
    echo "$LOG ERROR: PostgreSQL failed to become ready!"
    exit 1
fi

#############################################
# 4. Ensure Nginx service is enabled + started
#############################################
echo "$LOG Ensuring Nginx is enabled and running..."
sudo systemctl enable "${NGINX_SERVICE}" || true
sudo systemctl start "${NGINX_SERVICE}" || true

#############################################
# 5. Prepare Nginx runtime directories
#############################################
echo "$LOG Preparing Nginx runtime directories..."
mkdir -p /etc/nginx/conf.d
mkdir -p /var/www/apnafund

#############################################
# 6. Done
#############################################
echo "$LOG startup.sh completed successfully"
