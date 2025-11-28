param(
  [ValidateSet("dev","test","prod")]
  [string]$Env = "dev",

  [string]$Region  = "ap-south-1",
  [string]$Profile = "ApnaFundAdmin",

# Optional: override the instance-id if you want to target a specific EC2
  [string]$InstanceId = ""
)
# ---- ensure UTF-8 console so unicode from AWS CLI doesn't crash ----
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = New-Object System.Text.UTF8Encoding($false)
$env:PYTHONIOENCODING = 'utf-8'
chcp 65001 > $null

function Get-InstanceId {
  param($Env, $Region, $Profile, $InstanceId)

  if ($InstanceId) { return $InstanceId }

  # 1) Try terraform output first (recommended)
  try {
    $root   = Split-Path -Parent (Split-Path -Parent $PSScriptRoot) # .../infra
    $envDir = Join-Path $root "envs\$Env"
    Push-Location $envDir
    $tfJson = terraform output -json 2>$null
    Pop-Location

    if ($LASTEXITCODE -eq 0 -and $tfJson) {
      $obj = $tfJson | ConvertFrom-Json
      if ($obj.dev_instance_id.value) {
        return $obj.dev_instance_id.value
      }
    }
  } catch { }

  # 2) Fallback: look up by tag Name = "ApnaFund Dev Server"
  $name = if ($Env -eq "dev") { "ApnaFund Dev Server" } elseif ($Env -eq "test") { "ApnaFund Test Server" } else { "ApnaFund Prod Server" }
  $ec2 = aws ec2 describe-instances `
        --region $Region --profile $Profile `
        --filters "Name=tag:Name,Values=$name" "Name=instance-state-name,Values=running,stopped,stopping,pending" |
          ConvertFrom-Json

  $ids = @()
  foreach ($r in $ec2.Reservations) {
    foreach ($i in $r.Instances) { $ids += $i.InstanceId }
  }
  if (-not $ids -or $ids.Count -eq 0) {
    throw "No EC2 instance found with tag Name='$name'."
  }
  # Prefer running if multiple
  $running = @()
  foreach ($r in $ec2.Reservations) {
    foreach ($i in $r.Instances) {
      if ($i.State.Name -eq "running") { $running += $i.InstanceId }
    }
  }
  if ($running.Count -gt 0) { return $running[0] }
  return $ids[0]
}

try {
  $iid = Get-InstanceId -Env $Env -Region $Region -Profile $Profile -InstanceId $InstanceId
  Write-Host "Target EC2 InstanceId: $iid" -ForegroundColor Cyan
} catch {
  Write-Error $_.Exception.Message
  exit 1
}

# Ensure instance is in a valid state (start it if needed)
$state = (aws ec2 describe-instances --instance-ids $iid --region $Region --profile $Profile | ConvertFrom-Json).Reservations[0].Instances[0].State.Name
if ($state -eq "stopped" -or $state -eq "stopping") {
  Write-Host "Instance is $state. Starting it..." -ForegroundColor Yellow
  aws ec2 start-instances --instance-ids $iid --region $Region --profile $Profile | Out-Null
  Write-Host "Waiting for 'running'..." -ForegroundColor Yellow
  aws ec2 wait instance-running --instance-ids $iid --region $Region --profile $Profile
}

# Send a combined command:
#  1) disk setup (idempotent): formats /dev/nvme1n1 if needed and mounts it at /opt/apnafund/postgres
#  2) update_startup_from_s3.sh: pulls latest startup.sh & restarts apnafund-startup.service
$cmd = @"
sudo /opt/apnafund/bin/update_startup_from_s3.sh && sudo /usr/local/bin/apnafund-disk-setup.sh
"@.Trim()

Write-Host "Sending SSM command (disk-setup + startup refresh)..." -ForegroundColor Green
$send = aws ssm send-command `
  --document-name "AWS-RunShellScript" `
  --targets Key=instanceids,Values=$iid `
  --parameters commands="$cmd" `
  --region $Region --profile $Profile `
  --comment "ApnaFund: disk setup + startup refresh" |
        ConvertFrom-Json

$commandId = $send.Command.CommandId
Write-Host "CommandId: $commandId" -ForegroundColor Cyan

# Wait for completion and print output
aws ssm wait command-executed --command-id $commandId --instance-id $iid --region $Region --profile $Profile

# ----- Fetch outputs (capture raw) -----
Write-Host "`n==== Fetching SSM outputs (saved to files) ====" -ForegroundColor DarkCyan

$stdout = aws ssm get-command-invocation `
  --command-id $commandId `
  --instance-id $iid `
  --region $Region --profile $Profile `
  --query StandardOutputContent --output text 2>$null

$stderr = aws ssm get-command-invocation `
  --command-id $commandId `
  --instance-id $iid `
  --region $Region --profile $Profile `
  --query StandardErrorContent --output text 2>$null

# ----- Persist to UTF-8 files -----
$ts = (Get-Date).ToString('yyyyMMdd_HHmmss')
$stdoutFile = ".\ssm_stdout_${iid}_${ts}.txt"
$stderrFile = ".\ssm_stderr_${iid}_${ts}.txt"

# Use Out-File -Encoding utf8 to guarantee UTF-8 files
$stdout | Out-File -FilePath $stdoutFile -Encoding utf8
$stderr | Out-File -FilePath $stderrFile -Encoding utf8

Write-Host "Saved StandardOutputContent -> $stdoutFile" -ForegroundColor DarkCyan
Write-Host "Saved StandardErrorContent  -> $stderrFile" -ForegroundColor DarkRed


