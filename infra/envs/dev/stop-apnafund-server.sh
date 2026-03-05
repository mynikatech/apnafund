
########################################
# 1. Stop App Service (safe)
########################################
if systemctl list-unit-files | grep -q "apnafund.service"; then
    echo "$LOG Stopping apnafund.service..."
    sudo systemctl stop apnafund.service || echo "$LOG WARN: Failed to stop ApnaFund app"
else
    echo "$LOG ApnaFund service not installed."
fi
