param(
    [string]$InstanceId,
    [string]$Env = "dev",
    [string]$Profile = "jenkins-bot",
    [string]$Region = "ap-south-1",
    [switch]$Wait
)

$TagName = "ApnaFund Dev Server"

# Find EC2 instance automatically
$InstanceId = aws ec2 describe-instances `
    --filters "Name=tag:Name,Values=$TagName" `
    --query "Reservations[0].Instances[0].InstanceId" `
    --output text `
    --region $Region `
    --profile $Profile

if ($InstanceId -eq "None") {
    Write-Error "No EC2 instance found for Name=$TagName"
    exit 1
}

Write-Host "Using EC2 Instance: $InstanceId"

$Command = "/opt/apnafund/bin/deploy-app.sh $Env"

$cmdId = aws ssm send-command `
    --document-name "AWS-RunShellScript" `
    --targets "Key=InstanceIds,Values=$InstanceId" `
    --parameters "commands=$Command" `
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
