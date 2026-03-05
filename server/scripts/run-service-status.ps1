param(
    [string]$Region = "ap-south-1",
    [string]$Profile = "ApnaFundAdmin"
)

$TagName = "ApnaFund Dev Server"

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

Write-Host "Using EC2 Instance: $InstanceId"

# Send command
$CommandId = aws ssm send-command `
  --document-name "AWS-RunShellScript" `
  --targets Key=InstanceIds,Values=$InstanceId `
  --parameters commands=["systemctl status apnafund --no-pager"] `
  --region $Region `
  --profile $Profile `
  --query "Command.CommandId" `
  --output text

Write-Host "CommandId: $CommandId"

# Wait briefly
Start-Sleep -Seconds 5

# Fetch output
aws ssm get-command-invocation `
  --command-id $CommandId `
  --instance-id $InstanceId `
  --region $Region `
  --profile $Profile
