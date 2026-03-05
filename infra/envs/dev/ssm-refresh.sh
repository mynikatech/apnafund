#!/bin/bash
set -euo pipefail
echo "========== TEST =========="

#LOG="/var/log/apnafund/ssm-refresh.log"
#exec >> "$LOG" 2>&1

echo "========== SSM REFRESH START: $(date) =========="


########################################
# 1. Pull latest scripts from S3
########################################
if [ -x /opt/apnafund/bin/sync-scripts-from-s3.sh ]; then
    echo "[SSM] Syncing latest scripts from S3..."
    sudo /opt/apnafund/bin/sync-scripts-from-s3.sh || echo "[WARN] sync-scripts failed"
else
    echo "[ERROR] sync-scripts-from-s3.sh missing!"
fi


########################################
# 2. Re-run disk setup (idempotent)
########################################
if [ -x /opt/apnafund/bin/apnafund-disk-setup.sh ]; then
    echo "[SSM] Running disk setup..."
    sudo /opt/apnafund/bin/apnafund-disk-setup.sh || echo "[WARN] disk setup failed"
else
    echo "[WARN] disk setup script missing"
fi


########################################
# 3. Install PostgreSQL (SAFE + IDEMPOTENT)
########################################
if [ -x /opt/apnafund/bin/postgres-install.sh ]; then
    echo "[SSM] Checking PostgreSQL installation..."

    # Skip if psql already exists
    if command -v psql >/dev/null 2>&1; then
        echo "[SSM] PostgreSQL already installed — skipping."
    else
        echo "[SSM] Installing PostgreSQL (first-time setup)..."
        sudo bash /opt/apnafund/bin/postgres-install.sh || echo "[WARN] Postgres install failed"
    fi
else
    echo "[WARN] postgres-install.sh missing — cannot install PostgreSQL."
fi


########################################
# 4. Install / Update Nginx Config
########################################
if [ -x /opt/apnafund/bin/nginx-install.sh ]; then
    echo "[SSM] Running nginx-install.sh..."
    sudo bash /opt/apnafund/bin/nginx-install.sh || echo "[WARN] nginx install failed"
else
    echo "[WARN] nginx-install.sh missing"
fi


########################################
# 5. Install / Update SSL Config
########################################
if [ -x /opt/apnafund/bin/nginx-ssl-setup.sh ]; then
    echo "[SSM] Running nginx-ssl-setup.sh..."
    sudo bash /opt/apnafund/bin/nginx-ssl-setup.sh || echo "[WARN] nginx SSL setup failed"
else
    echo "[WARN] nginx-ssl-setup.sh missing"
fi


########################################
# 6. Reload systemd (needed if any services updated)
########################################
echo "[SSM] Reloading systemd..."
sudo systemctl daemon-reload || true


########################################
# 7. Restart nginx (safe)
########################################
echo "[SSM] Restarting nginx..."
sudo systemctl restart nginx || echo "[WARN] nginx restart failed"


echo "========== SSM REFRESH COMPLETE: $(date) =========="
