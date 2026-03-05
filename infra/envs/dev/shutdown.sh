#!/bin/bash
set -euo pipefail

LOG="[shutdown]"

echo "$LOG Stopping ApnaFund services..."

SYSTEMCTL="/usr/bin/systemctl --system"

########################################
# 1. Stop Nginx
########################################
if $SYSTEMCTL is-active --quiet nginx; then
    echo "$LOG Stopping nginx..."
    sudo $SYSTEMCTL stop nginx || echo "$LOG WARN: Failed to stop nginx"
else
    echo "$LOG nginx not running (or not installed)."
fi

########################################
# 2. Stop PostgreSQL 17
########################################
if $SYSTEMCTL is-active --quiet postgresql-17; then
    echo "$LOG Stopping PostgreSQL 17..."
    sudo $SYSTEMCTL stop postgresql-17 || echo "$LOG WARN: Failed to stop PostgreSQL"
else
    echo "$LOG PostgreSQL 17 not running (or not installed)."
fi

echo "$LOG Shutdown complete."
