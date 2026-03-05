<#
.SYNOPSIS
  Unified Terraform environment manager for ApnaFund infrastructure.
.DESCRIPTION
  Handles bootstrap, dev, test, and prod Terraform actions with logging,
  without changing the user's working directory.
.PARAMETER Env
  One of: bootstrap, dev, test, prod
.PARAMETER Action
  One of: deploy, destroy, plan, validate
. PARAMETER AWSProfile
    default - ApnaFundAdmin
#>

param(
    [Parameter(Mandatory)][ValidateSet("bootstrap", "dev", "test", "prod")] [string]$Env,
    [Parameter(Mandatory)][ValidateSet("deploy", "destroy", "plan", "validate")] [string]$Action,
    [Parameter(Mandatory=$false)] [string]$AwsProfile = ""
)

# --- Setup -------------------------------------------------------------
$root = Split-Path -Parent $PSScriptRoot
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$logDir = Join-Path $root "logs"
if (!(Test-Path $logDir)) { New-Item -ItemType Directory -Force -Path $logDir | Out-Null }
$logFile = Join-Path $logDir "$Env-$Action-$timestamp.log"

if ($AwsProfile -ne "") {
    Write-Host "[INFO] Using AWS profile: $AwsProfile"
    $env:AWS_PROFILE = $AwsProfile
}
else {
    Write-Host "[INFO] Using existing AWS profile: $env:AWS_PROFILE"
}

# Determine working directory
if ($Env -eq "bootstrap") {
    $workDir = Join-Path $root "global\bootstrap"
} else {
    $workDir = Join-Path $root "envs\$Env"
}

Write-Host "=== Running Terraform $Action for environment: $Env ===" -ForegroundColor Cyan
Write-Host "Working directory: $workDir" -ForegroundColor DarkGray
Write-Host "Log file: $logFile" -ForegroundColor DarkGray
Write-Host ""

# --- Helper: run terraform safely without leaving scripts dir ----------
function Run-Terraform($cmd) {
    Write-Host ">>> $cmd" -ForegroundColor Yellow
    Push-Location $workDir
    try {
        & cmd.exe /c "$cmd 2>&1"
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Command failed: $cmd" -ForegroundColor Red
            Write-Host "Check log file: $logFile" -ForegroundColor Red
            exit 1
        }
    }
    finally {
        Pop-Location
    }
}
# Precompute absolute paths for clarity
$backendCfg = Join-Path $workDir "backend.hcl"
$tfvarsFile = Join-Path $workDir "$Env.tfvars"

# --- Actions -----------------------------------------------------------
if ($Env -eq "bootstrap") {
    switch ($Action) {
        "plan" {
            Run-Terraform "terraform init -backend=false"
            Run-Terraform "terraform plan"
        }
        "deploy" {
            Run-Terraform "terraform init -backend=false"
            Run-Terraform "terraform apply -auto-approve"
        }
        "destroy" {
            Run-Terraform "terraform init -backend=false"
            Run-Terraform "terraform destroy -auto-approve"
        }
        "validate" {
            Run-Terraform "terraform init -backend=false"
            Run-Terraform "terraform validate"
        }
    }
}
else {
    switch ($Action) {
        "plan" {
            Run-Terraform "terraform init -reconfigure -backend-config=`"$backendCfg`""
            Run-Terraform "terraform plan -lock-timeout=60s -var-file=`"$tfvarsFile`""
        }
        "deploy" {
            Run-Terraform "terraform init -reconfigure -backend-config=`"$backendCfg`""
            Run-Terraform "terraform plan -lock-timeout=60s -var-file=`"$tfvarsFile`" -out=tfplan"
            Run-Terraform "terraform apply tfplan"
        }
        "destroy" {
            Run-Terraform "terraform init -reconfigure -backend-config=`"$backendCfg`""
            Run-Terraform "terraform destroy -lock-timeout=60s -var-file=`"$tfvarsFile`" -auto-approve"
        }
        "validate" {
            Run-Terraform "terraform init -reconfigure -backend-config=`"$backendCfg`""
            Run-Terraform "terraform validate"
        }
    }
}

Write-Host ""
Write-Host "=== Terraform $Action for $Env completed successfully! ===" -ForegroundColor Green
Write-Host "Log file saved at: $logFile" -ForegroundColor DarkGray
Write-Host "Returned to: $PSScriptRoot" -ForegroundColor DarkGray
