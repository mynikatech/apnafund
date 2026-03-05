<#
Initial DB Setup — Minimal & Reliable
-------------------------------------
Prerequisites:
1. You already started the SSM tunnel manually:
      .\start-ssm-tunnel.ps1 -Env dev -profile ApnaFundAdmin
2. PostgreSQL is reachable at localhost:15432

This script does ONLY:
 - Fetch secrets
 - Create admin role if missing
 - Create database if missing
#>

[CmdletBinding()]
param(
    [ValidateSet('dev','test','prod')] [string]$Env = "dev",
    [string]$AwsProfile = "default",
    [string]$SecretName = "apnafund/dev/postgres",
    [string]$Region = "ap-south-1"
)

Write-Host "`n=== Initial DB Setup ===" -ForegroundColor Cyan
Write-Host "Using existing SSM tunnel on 127.0.0.1:15432" -ForegroundColor Yellow

# -------- 1. Fetch Secrets ---------
Write-Host "`nFetching admin credentials..." -ForegroundColor Cyan

$cmd = "aws secretsmanager get-secret-value --secret-id `"$SecretName`" --query SecretString --output text --region $Region --profile $AwsProfile"
$secretJson = Invoke-Expression $cmd

if (-not $secretJson) {
    Write-Error "Could not fetch DB secret"
    exit 1
}

$secret = $secretJson | ConvertFrom-Json
$adminUser = $secret.admin_user
$adminPass = $secret.admin_pass

Write-Host " → Admin User: $adminUser"

# -------- 2. Build SQL File ---------
$tempSql = Join-Path $env:TEMP "initdb.sql"

$sql = @"
-- Create admin role if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '$adminUser') THEN
        EXECUTE format(
            'CREATE ROLE %I LOGIN PASSWORD %L SUPERUSER;',
            '$adminUser', '$adminPass'
        );
    END IF;
END;
$$;

-- Create database if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'apnafund') THEN
        EXECUTE 'CREATE DATABASE apnafund OWNER $adminUser';
    END IF;
END;
$$;
"@

# Write file as UTF-8 **without BOM**
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($tempSql, $sql, $utf8NoBom)

Write-Host "Generated SQL: $tempSql"

# -------- 3. Run SQL via SSM Tunnel ---------
Write-Host "`nRunning SQL on PostgreSQL..." -ForegroundColor Cyan

$psqlCmd = "psql -h 127.0.0.1 -p 15432 -U postgres -f `"$tempSql`""
Write-Host "Executing: $psqlCmd"

$env:PGPASSWORD = ""   # postgres local superuser password (empty if trust auth enabled)

# Run and capture output
$process = Start-Process psql -ArgumentList "-h","127.0.0.1","-p","15432","-U","postgres","-f",$tempSql `
    -NoNewWindow -Wait -PassThru

if ($process.ExitCode -ne 0) {
    Write-Error "` SQL execution failed. Database NOT initialized."
    exit $process.ExitCode
}

# -------- 4. Confirm Results ---------
Write-Host "`n=== Initial DB Setup Complete ===" -ForegroundColor Green
Write-Host " ✓ Admin Role ensured: $adminUser"
Write-Host " ✓ Database ensured: apnafund"
Write-Host "Next: Run Liquibase baselineSchema"
