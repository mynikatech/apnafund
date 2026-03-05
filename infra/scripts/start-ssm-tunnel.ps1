param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("dev","test","prod")]
    [string]$Env,

    [string]$Region = "ap-south-1",
    [string]$Profile = "ApnaFundAdmin"
)
$Env = $Env.Substring(0,1).ToUpper() + $Env.Substring(1).ToLower()
# Tag names follow convention e.g. "ApnaFund Dev Server"
$TagKey = "Name"
$TagValue = "ApnaFund $Env Server"

Write-Host "Looking for EC2 instance tagged $TagKey=$TagValue in $Region..." -ForegroundColor Cyan
$instanceId = (aws ec2 describe-instances `
  --region $Region --profile $Profile `
  --filters "Name=tag:$TagKey,Values=$TagValue" "Name=instance-state-name,Values=running" `
  --query "Reservations[0].Instances[0].InstanceId" --output text)

if (-not $instanceId -or $instanceId -eq "None") {
    Write-Host "No running EC2 instance found for env '$Env' ($TagKey=$TagValue)" -ForegroundColor Red
    exit 1
}

Write-Host "Found instance: $instanceId" -ForegroundColor Green
Write-Host "Starting SSM tunnel localhost:15432 → EC2:5432 for $Env" -ForegroundColor Yellow

aws ssm start-session `
  --target $instanceId `
  --document-name AWS-StartPortForwardingSession `
  --parameters portNumber="5432",localPortNumber="15432" `
  --region $Region --profile $Profile
