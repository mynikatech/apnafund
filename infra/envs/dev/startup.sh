#!/bin/bash
set -euo pipefail

APNA_ENV="${APNA_ENV:-dev}"
PG_IMAGE="${PG_IMAGE:-postgres:17.2}"   # pin by minor or use a digest
PG_DATA_DIR="/opt/apnafund/postgres"    # or "/pgdata" if you mounted EBS there

# Load .env if present
if [ -f /opt/apnafund/.env ]; then
  set -a; source /opt/apnafund/.env; set +a
fi

mkdir -p "$PG_DATA_DIR"

# Pull pinned image (idempotent)
docker pull "$PG_IMAGE" || true

# Start or restart Postgres
if docker ps --format '{{.Names}}' | grep -q '^apnafund-postgres$'; then
  echo "[startup] Postgres already running"
else
  if docker ps -a --format '{{.Names}}' | grep -q '^apnafund-postgres$'; then
    docker rm -f apnafund-postgres || true
  fi
  docker run -d \
    --name apnafund-postgres \
    -e POSTGRES_DB="${POSTGRES_DB:-apnafund}" \
    -e POSTGRES_USER="${POSTGRES_ADMIN_USER:-apnafund_admin}" \
    -e POSTGRES_PASSWORD="${POSTGRES_ADMIN_PASS:-changeme}" \
    -v "${PG_DATA_DIR}:/var/lib/postgresql/data" \
    -p 5432:5432 \
    --restart unless-stopped \
    "$PG_IMAGE"
fi

# Wait until ready
for i in {1..30}; do
  if docker exec apnafund-postgres pg_isready -U "${POSTGRES_ADMIN_USER:-apnafund_admin}" -d "${POSTGRES_DB:-apnafund}" >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

echo "[startup] Postgres ready."
