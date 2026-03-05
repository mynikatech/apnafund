param(
    [string]$Env = "dev",
    [string]$Region = "ap-south-1",
    [string]$Profile = "ApnaFundAdmin"
)

$TagName = "ApnaFund Dev Server"

Write-Host "Locating EC2 instance with tag Name=$TagName..."
$InstanceId = (aws ec2 describe-instances `
    --filters "Name=tag:Name,Values=$TagName" `
    --query "Reservations[0].Instances[0].InstanceId" `
    --output text `
    --region $Region `
    --profile $Profile)

if ($InstanceId -eq "None") {
    Write-Error "No instance found"
    exit 1
}

Write-Host "Using instance: $InstanceId"

$Bucket = "apnafund-app-861082243595-$Region"
$Key    = "apnafund/$Env/config/sync-app-scripts-from-s3.sh"

# Build parameters object
$commands = @(
        "aws s3 cp s3://$Bucket/$Key /opt/apnafund/bin/sync-app-scripts-from-s3.sh",
        "chmod +x /opt/apnafund/bin/sync-app-scripts-from-s3.sh",
        "bash /opt/apnafund/bin/sync-app-scripts-from-s3.sh $Env"
)

$cmdJson = ($commands | ConvertTo-Json -Compress)

Write-Host "DEBUG: commands array content:"
$commands
Write-Host "DEBUG: commands Json to be send:"
$cmdJson

Write-Host "Sending SSM command..."

$send = aws ssm send-command `
    --document-name "AWS-RunShellScript" `
    --targets Key=instanceids,Values=$InstanceId `
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

aws ssm wait command-executed `
    --command-id $commandId `
    --instance-id $InstanceId `
    --region $Region `
    --profile $Profile

Write-Host "Bootstrap sync completed successfully."
