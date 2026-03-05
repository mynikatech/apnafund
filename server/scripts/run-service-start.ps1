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

Write-Host "Starting apnafund service on $InstanceId"

aws ssm send-command `
  --document-name "AWS-RunShellScript" `
  --targets Key=InstanceIds,Values=$InstanceId `
  --parameters commands=["systemctl start apnafund"] `
  --region $Region `
  --profile $Profile
