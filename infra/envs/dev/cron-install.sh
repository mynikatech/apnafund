#!/bin/bash
set -euo pipefail

LOG="[cron-install]"

echo "$LOG Starting..."

if command -v crontab >/dev/null 2>&1; then
    echo "$LOG cronie already installed."
else
    echo "$LOG Installing cronie..."
    dnf install -y cronie
fi

systemctl enable crond

echo "$LOG Installation complete."