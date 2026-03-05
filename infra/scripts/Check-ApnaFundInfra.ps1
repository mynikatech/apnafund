param(
    [string]$InstanceId = "i-090127af9e3c0f572",
    [string]$Profile = "ApnaFundAdmin",
    [string]$Region = "ap-south-1"
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8

function Run-SSMCommand($cmd) {
    $escapedCmd = $cmd -replace "'", "'\''" -replace '"', '\"'
    $send = aws ssm send-command `
        --document-name "AWS-RunShellScript" `
        --instance-ids $InstanceId `
        --profile $Profile `
        --region $Region `
        --parameters "commands=`"$escapedCmd`"" `
        --query "Command.CommandId" `
        --output text

    Start-Sleep -Seconds 3

    $out = aws ssm get-command-invocation `
        --command-id $send `
        --instance-id $InstanceId `
        --profile $Profile `
        --region $Region `
        --query "StandardOutputContent" `
        --output text

    if (-not $out) { return "<no output>" }
    return $out
}

function Decode($rawOutput) {
    if ([string]::IsNullOrWhiteSpace($rawOutput)) { return "<no output>" }

    # Try UTF8 first (CLI v2 raw binary)
    try {
        $utf8Bytes = [System.Text.Encoding]::UTF8.GetBytes($rawOutput)
        return [System.Text.Encoding]::UTF8.GetString($utf8Bytes)
    }
    catch { }

    # Fallback to base64 (CLI v1 or explicit base64)
    try {
        return [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($rawOutput))
    }
    catch {
        return "<decode failed: $_>"
    }
}

Write-Host "====================================="
Write-Host "  Checking ApnaFund EC2 Infrastructure"
Write-Host "====================================="

Write-Host "`n[1] Checking EBS Volume..."
Decode (Run-SSMCommand "lsblk") | Write-Host

Write-Host "`n[2] Checking Mount Points..."
Decode (Run-SSMCommand "df -h") | Write-Host

Write-Host "`n[3] Checking PostgreSQL Installation..."
Decode (Run-SSMCommand "psql --version || echo PostgreSQL_NOT_installed") | Write-Host

Write-Host "`n[4] Checking PostgreSQL service..."
Decode (Run-SSMCommand "systemctl is-active postgresql && systemctl status postgresql --no-pager -l | head -15") | Write-Host

Write-Host "`n[5] Checking NGINX Installation..."
Decode (Run-SSMCommand "nginx -v 2>&1 || echo NGINX_NOT_installed") | Write-Host

Write-Host "`n[6] Checking NGINX Service..."
Decode (Run-SSMCommand "systemctl is-active nginx && systemctl status nginx --no-pager -l | head -15") | Write-Host
Write-Host "`nDone."
