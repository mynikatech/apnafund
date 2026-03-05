param(
    [string]$Env = "dev",
    [string]$InstanceId,
    [string]$Domain,
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

./upload-config.ps1 -Env $Env

./sync-scripts.ps1 -InstanceId $InstanceId -Wait:$WaitAll
./run-app-setup.ps1 -InstanceId $InstanceId -Wait:$WaitAll
./run-generate-cert.ps1 -InstanceId $InstanceId -Env $Env -Domain $Domain -Wait:$WaitAll
./run-deploy-nginx.ps1 -InstanceId $InstanceId -Env $Env -Wait:$WaitAll

Write-Host "Environment setup completed successfully."
