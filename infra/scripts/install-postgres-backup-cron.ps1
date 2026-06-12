param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("dev","test","prod")]
    [string]$Env,

    [string]$Region = "ap-south-1",
    [string]$Profile = "ApnaFundAdmin"
)

$Env = $Env.Substring(0,1).ToUpper() + $Env.Substring(1).ToLower()

$TagKey = "Name"
$TagValue = "ApnaFund $Env Server"

Write-Host "Looking for EC2 instance tagged $TagKey=$TagValue in $Region..." -ForegroundColor Cyan

$instanceId = aws ec2 describe-instances `
  --region $Region `
  --profile $Profile `
  --filters "Name=tag:$TagKey,Values=$TagValue" "Name=instance-state-name,Values=running" `
  --query "Reservations[0].Instances[0].InstanceId" `
  --output text

if (-not $instanceId -or $instanceId -eq "None") {
    Write-Host "No running EC2 instance found." -ForegroundColor Red
    exit 1
}

Write-Host "Found instance: $instanceId" -ForegroundColor Green

aws ssm send-command `
  --instance-ids $instanceId `
  --document-name "AWS-RunShellScript" `
  --parameters commands='["sudo bash /opt/apnafund/bin/install-postgres-backup-cron.sh"]' `
  --region $Region `
  --profile $Profile