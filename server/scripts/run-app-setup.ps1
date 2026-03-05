param(
    [string]$Region = "ap-south-1",
    [string]$Profile = "ApnaFundAdmin"
)

$TagName = "ApnaFund Dev Server"

Write-Host "Locating EC2 instance for tag Name=$TagName"

$InstanceId = aws ec2 describe-instances `
    --filters "Name=tag:Name,Values=$TagName" `
    --query "Reservations[0].Instances[0].InstanceId" `
    --output text `
    --region $Region `
    --profile $Profile

if ($InstanceId -eq "None" -or $InstanceId -eq $null) {
    Write-Error "No EC2 instance found with Name=$TagName"
    exit 1
}

Write-Host "EC2 Instance found: $InstanceId"

$CommandPath = "/opt/apnafund/bin/app-setup.sh"


Write-Host "Sending SSM command..."

$cmdId = aws ssm send-command `
    --document-name "AWS-RunShellScript" `
    --targets "Key=InstanceIds,Values=$InstanceId" `
    --parameters commands="$CommandPath" `
    --region $Region `
    --profile $Profile `
    --query "Command.CommandId" `
    --output text

Write-Host "SSM CommandId: $cmdId"

Write-Host "Waiting for command execution..."

aws ssm wait command-executed `
    --command-id $cmdId `
    --instance-id $InstanceId `
    --region $Region `
    --profile $Profile

$invocationStatus = aws ssm get-command-invocation `
    --command-id $cmdId `
    --instance-id $InstanceId `
    --region $Region `
    --profile $Profile `
    --query "Status" `
    --output text

Write-Host "Command Status: $invocationStatus"

if ($invocationStatus -ne "Success") {
    Write-Host "SSM command failed. Fetching logs..."
    aws ssm get-command-invocation `
        --command-id $cmdId `
        --instance-id $InstanceId `
        --region $Region `
        --profile $Profile `
        --query "StandardErrorContent" `
        --output text
    exit 1
}

Write-Host "app-setup.sh completed successfully on EC2 instance."
