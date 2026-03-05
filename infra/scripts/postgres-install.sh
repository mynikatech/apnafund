#!/bin/bash
set -euo pipefail

# -------- Logging Setup --------
mkdir -p /var/log/apnafund
LOG="/var/log/apnafund/postgres-install.log"
exec >> "$LOG" 2>&1

echo "========== PostgreSQL INSTALL START: $(date) =========="

# -------- Paths & Settings --------
PG_MAJOR=17
PGUSER="postgres"
PGDATA="/opt/apnafund/postgres"

# CORRECT binary location for Amazon Linux 2023 PGDG
PGBIN="/usr/bin"

SERVICE_NAME="postgresql-${PG_MAJOR}"
SERVICE_FILE="/etc/systemd/system/${SERVICE_NAME}.service"

echo "[INFO] Using PGDATA=$PGDATA"
echo "[INFO] Using PGBIN=$PGBIN"
echo "[INFO] Service name: $SERVICE_NAME"

########################################
# 1. Ensure postgres user exists
########################################
if ! id "$PGUSER" >/dev/null 2>&1; then
    echo "[INFO] Creating postgres user..."
    useradd -r -s /bin/bash postgres
else
    echo "[INFO] postgres user already exists."
fi

########################################
# 2. Install PostgreSQL 17 if missing
########################################
if ! command -v psql >/dev/null 2>&1; then
    echo "[INFO] Installing PostgreSQL 17..."

    dnf install -y https://download.postgresql.org/pub/repos/yum/reporpms/EL-9-x86_64/pgdg-redhat-repo-latest.noarch.rpm
    dnf -qy module disable postgresql
    dnf install -y postgresql17 postgresql17-server postgresql17-contrib

    echo "[INFO] PostgreSQL 17 installed."
else
    echo "[INFO] PostgreSQL 17 already installed."
fi

########################################
# 3. Ensure PGDATA exists and permissions correct
########################################
echo "[INFO] Creating PGDATA directory..."
mkdir -p "$PGDATA"
chown -R postgres:postgres "$PGDATA"
chmod 700 "$PGDATA"

########################################
# 4. Initialize database only once
########################################
if [ ! -f "$PGDATA/PG_VERSION" ]; then
    echo "[INFO] Initializing PostgreSQL database..."
    su - postgres -c "${PGBIN}/initdb -D ${PGDATA}"
else
    echo "[INFO] PGDATA already initialized."
fi

########################################
# 5. Ensure socket directory exists
########################################
echo "[INFO] Creating PostgreSQL runtime directory..."
mkdir -p /run/postgresql
chown postgres:postgres /run/postgresql
chmod 775 /run/postgresql

########################################
# 6. Ensure logfile exists
########################################
echo "[INFO] Ensuring logfile exists..."
touch ${PGDATA}/logfile
chown postgres:postgres ${PGDATA}/logfile

########################################
# 7. Create systemd service
########################################
if [ ! -f "$SERVICE_FILE" ]; then
    echo "[INFO] Creating systemd service file: $SERVICE_FILE"

tee "$SERVICE_FILE" > /dev/null <<EOF
[Unit]
Description=PostgreSQL ${PG_MAJOR} database server
After=network.target

[Service]
Type=forking
User=postgres
Group=postgres
Environment=PGDATA=${PGDATA}
ExecStart=${PGBIN}/pg_ctl start -D ${PGDATA} -s -l ${PGDATA}/logfile -o "-c listen_addresses='*'"
ExecStop=${PGBIN}/pg_ctl stop -D ${PGDATA} -s -m fast
ExecReload=${PGBIN}/pg_ctl reload -D ${PGDATA} -s
TimeoutSec=300

[Install]
WantedBy=multi-user.target
EOF

else
    echo "[INFO] Service file already exists."
fi

########################################
# 8. Start + enable PostgreSQL using systemd
########################################
echo "[INFO] Reloading systemd..."
systemctl daemon-reload

echo "[INFO] Starting PostgreSQL service..."
systemctl restart "$SERVICE_NAME" || true

echo "[INFO] Enabling PostgreSQL to run on boot..."
systemctl enable "$SERVICE_NAME" || true

########################################
# 9. Verify service is running
########################################
echo "[INFO] Checking service status..."
if systemctl is-active --quiet "$SERVICE_NAME"; then
    echo "[SUCCESS] PostgreSQL ${PG_MAJOR} is running."
else
    echo "[ERROR] PostgreSQL ${PG_MAJOR} failed to start. Check ${PGDATA}/logfile"
fi

echo "========== PostgreSQL INSTALL COMPLETE: $(date) =========="
