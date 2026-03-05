param(
    [string]$Env = "dev",
    [string]$InstanceId,
    [switch]$WaitAll
)

$TagName = "ApnaFund Dev Server"

# Find EC2 instance automatically
$InstanceId = (aws ec2 describe-instances `
    --filters "Name=tag:Name,Values=$TagName" `
    --query "Reservations[0].Instances[0].InstanceId" `
    --output text `
    --region $Region)

if ($InstanceId -eq "None") {
    Write-Error "❌ No EC2 instance found for Name=$TagName"
    exit 1
}

Write-Host "✔ Using EC2 Instance: $InstanceId"

./build-jar.ps1
./upload-artifacts.ps1 -Env $Env

./sync-scripts.ps1 -InstanceId $InstanceId -Wait:$WaitAll

./run-deploy-app.ps1 -InstanceId $InstanceId -Env $Env -Wait:$WaitAll

Write-Host "Deploy completed successfully."
