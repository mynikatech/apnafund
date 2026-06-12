#!/bin/bash
set -euo pipefail

CRON_FILE="/etc/cron.d/apnafund-postgres-backup"

cat > "$CRON_FILE" <<EOF
0 2 * * * root /opt/apnafund/bin/postgres-backup.sh >> /var/log/apnafund/postgres-backup.log 2>&1
EOF

chmod 644 "$CRON_FILE"

systemctl restart cron 2>/dev/null || true
systemctl restart crond 2>/dev/null || true

echo "Postgres backup cron installed"