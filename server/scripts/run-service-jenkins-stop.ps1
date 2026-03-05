param(
    [string]$Region = "ap-south-1",
    [string]$Profile = "jenkins-bot"
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

Write-Host "Stopping apnafund service on $InstanceId"

aws ssm send-command `
  --document-name "AWS-RunShellScript" `
  --targets Key=InstanceIds,Values=$InstanceId `
  --parameters commands=["systemctl stop apnafund"] `
  --region $Region `
  --profile $Profile
