param(
    [ValidateSet("dev","test","prod")]
    [string]$Env = "dev",

    [string]$Region  = "ap-south-1",

# Optional — override instance-id
    [string]$InstanceId = "",

# NEW — allow passing profile
    [string]$Profile = "default"
)

# -------------------------
# Updated credential loader
# -------------------------
Write-Host "[INFO] Loading AWS credentials using profile: $Profile"

$accessKey = aws configure get aws_access_key_id --profile $Profile
$secretKey = aws configure get aws_secret_access_key --profile $Profile
$sessionToken = aws configure get aws_session_token --profile $Profile

if (-not $accessKey -or -not $secretKey) {
    Write-Error "[FATAL] No AWS credentials found for profile '$Profile'!"
    exit 1
}

$env:AWS_ACCESS_KEY_ID = $accessKey
$env:AWS_SECRET_ACCESS_KEY = $secretKey
if ($sessionToken) { $env:AWS_SESSION_TOKEN = $sessionToken }

Write-Host "[INFO] Credentials loaded successfully."
Write-Host "AWS_ACCESS_KEY_ID: $env:AWS_ACCESS_KEY_ID"

# -------------------------
# 1) Get InstanceId
# -------------------------
function Get-InstanceId {
    param($Env, $Region, $InstanceId)

    if ($InstanceId) { return $InstanceId }

    try {
        $root   = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
        $envDir = Join-Path $root "envs\$Env"

        Push-Location $envDir
        $tfJson = terraform output -json 2>$null
        Pop-Location

        if ($LASTEXITCODE -eq 0 -and $tfJson) {
            $obj = $tfJson | ConvertFrom-Json
            if ($obj.dev_instance_id.value) {
                Write-Host "[INFO] Terraform InstanceId: $($obj.dev_instance_id.value)"
                return $obj.dev_instance_id.value
            }
        }
    }
    catch {}

    throw "[ERROR] No instance-id available!"
}

# -------------------------
# Resolve the instanceId
# -------------------------
try {
    $iid = Get-InstanceId -Env $Env -Region $Region -InstanceId $InstanceId
    Write-Host "Target EC2 InstanceId: $iid" -ForegroundColor Cyan
}
catch {
    Write-Error $_.Exception.Message
    exit 1
}

# -------------------------
# Check EC2 state
# -------------------------
Write-Host "[INFO] Checking EC2 state..."
$stateJson = aws ec2 describe-instances `
    --instance-ids $iid --region $Region |
        ConvertFrom-Json

if (-not $stateJson) {
    Write-Error "[ERROR] aws describe-instances returned no data."
    exit 1
}

$state = $stateJson.Reservations[0].Instances[0].State.Name
Write-Host "[INFO] State: $state"

if ($state -eq "stopped" -or $state -eq "stopping") {
    Write-Host "Instance is $state. Starting..."
    aws ec2 start-instances --instance-ids $iid --region $Region | Out-Null
    aws ec2 wait instance-running --instance-ids $iid --region $Region
}

# -------------------------
# SSM Commands
# -------------------------
$commands = @(
    "set -ex",

    # Always download the latest sync-scripts-from-s3.sh
    "aws s3 cp s3://apnafund-config-861082243595-ap-south-1/apnafund/$Env/sync-scripts-from-s3.sh /opt/apnafund/bin/sync-scripts-from-s3.sh",
    "sudo chmod +x /opt/apnafund/bin/sync-scripts-from-s3.sh",

    # Run it so it pulls all other scripts
    "sudo bash /opt/apnafund/bin/sync-scripts-from-s3.sh",

    # Now run the refresh master orchestrator
    "sudo bash /opt/apnafund/bin/ssm-refresh.sh"
)
$cmdJson = ($commands | ConvertTo-Json -Compress)

Write-Host "DEBUG: commands array content:"
$commands
Write-Host "DEBUG: commands Json to be send:"
$cmdJson

$send = aws ssm send-command `
    --document-name "AWS-RunShellScript" `
    --targets Key=instanceids,Values=$iid `
    --parameters commands="$cmdJson" `
    --region $Region `
    --comment "ApnaFund: Full refresh" |
        ConvertFrom-Json

$commandId = $send.Command.CommandId
if (-not $commandId) {
    Write-Error "[ERROR] SSM did not return a commandId."
    exit 1
}

Write-Host "CommandId: $commandId"

# -------------------------
# Wait for command completion
# -------------------------
aws ssm wait command-executed `
    --command-id $commandId `
    --instance-id $iid `
    --region $Region

# -------------------------
# Fetch Output
# -------------------------
$ts = (Get-Date).ToString("yyyyMMdd_HHmmss")
$stdoutFile = ".\ssm_stdout_${iid}_${ts}.txt"
$stderrFile = ".\ssm_stderr_${iid}_${ts}.txt"

aws ssm get-command-invocation `
    --command-id $commandId `
    --instance-id $iid `
    --region $Region `
    --query StandardOutputContent `
    --output text | Out-File $stdoutFile -Encoding utf8

aws ssm get-command-invocation `
    --command-id $commandId `
    --instance-id $iid `
    --region $Region `
    --query StandardErrorContent `
    --output text | Out-File $stderrFile -Encoding utf8

Write-Host "Saved stdout -> $stdoutFile"
Write-Host "Saved stderr -> $stderrFile"
