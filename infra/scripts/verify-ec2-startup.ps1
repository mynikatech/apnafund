# verify-ec2-startup-hardcoded.ps1
# Simple EC2 startup verification using a hardcoded IP.
# Edit $InstanceIp and $KeyPath below, then run:
#   .\verify-ec2-startup-hardcoded.ps1

# === EDIT THESE ===
$InstanceIp = "13.127.169.186"                         # <- put your EC2 public IP here
$KeyPath    = "..\envs\dev\apnafund-dev-key.pem"    # <- path to your .pem (adjust if needed)
# ==================

if ($InstanceIp -eq "" -or $InstanceIp -like "<*") {
    Write-Host "Please set the \$InstanceIp variable at the top of this script." -ForegroundColor Yellow
    exit 1
}

if (-not (Test-Path $KeyPath)) {
    Write-Host "Key file not found: $KeyPath" -ForegroundColor Red
    exit 1
}

Write-Host "Connecting to $InstanceIp using key $KeyPath" -ForegroundColor Cyan

# single-quoted command string (keeps quoting simple)
$remoteCmd = 'lsblk; echo "-----"; df -h | grep apnafund || echo "No apnafund mount found."; echo "-----"; systemctl status apnafund-startup.service --no-pager || echo "Service not found."; echo "-----"; sudo journalctl -u apnafund-startup.service -n 200 --no-pager; echo "-----"; sudo tail -n 200 /var/log/apnafund/startup.log || echo "startup.log not found."'

try {
    & ssh "-i" $KeyPath "ec2-user@$InstanceIp" $remoteCmd
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ssh exited with code $LASTEXITCODE" -ForegroundColor Yellow
    }
} catch {
    $msg = if ($_.Exception -and $_.Exception.Message) { $_.Exception.Message } else { $_.ToString() }
    Write-Host "SSH failed:" $msg -ForegroundColor Red
}
