param(
    [ValidateSet("start", "stop", "restart", "status")]
    [string]$Action = "status",

    [string]$Region = "ap-south-1",
    [string]$Profile = "ApnaFundAdmin",

    [string]$TagName = "ApnaFund Dev Server"
)

Write-Host "Action      : $Action"
Write-Host "Region      : $Region"
Write-Host "AWS Profile : $Profile"
Write-Host "Target Tag  : $TagName"
Write-Host ""

############################################
# 1. Resolve EC2 instance
############################################
$InstanceId = aws ec2 describe-instances `
    --filters "Name=tag:Name,Values=$TagName" `
    --query "Reservations[0].Instances[0].InstanceId" `
    --output text `
    --region $Region `
    --profile $Profile

if (!$InstanceId -or $InstanceId -eq "None") {
    Write-Error "No EC2 instance found for tag '$TagName'"
    exit 1
}

Write-Host "Using EC2 Instance: $InstanceId"

############################################
# 2. Build command list
############################################
switch ($Action) {
    "start" {
        $Commands = @(
            "sudo systemctl start postgresql-17",
            "sudo systemctl status postgresql-17 --no-pager"
        )
    }
    "stop" {
        $Commands = @(
            "sudo systemctl stop postgresql-17",
            "sudo systemctl status postgresql-17 --no-pager"
        )
    }
    "restart" {
        $Commands = @(
            "sudo systemctl restart postgresql-17",
            "sleep 2",
            "sudo systemctl status postgresql-17 --no-pager"
        )
    }
    "status" {
        $Commands = @(
            "sudo systemctl status postgresql-17 --no-pager"
        )
    }
}

############################################
# 3. Send SSM command
############################################
$CommandId = aws ssm send-command `
    --document-name "AWS-RunShellScript" `
    --targets "Key=InstanceIds,Values=$InstanceId" `
    --parameters "commands=$($Commands | ConvertTo-Json -Compress)" `
    --region $Region `
    --profile $Profile `
    --query "Command.CommandId" `
    --output text

Write-Host "CommandId: $CommandId"

############################################
# 4. Wait & fetch output
############################################
Start-Sleep -Seconds 2

aws ssm get-command-invocation `
    --command-id $CommandId `
    --instance-id $InstanceId `
    --region $Region `
    --profile $Profile
