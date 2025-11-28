<#
Unified Liquibase runner for ApnaBank
Examples:
  .\run_liquibase.ps1 -Env test -Command update -Labels baseline
  .\run_liquibase.ps1 -Env test -Command update -DryRunThenConfirm
  .\run_liquibase.ps1 -Env prod -Command status
  .\run_liquibase.ps1 -Env test -Command rollbackCount -RollbackCount 2
  .\run_liquibase.ps1 -Env test -Command tag -RollbackTag baseline-1.0 -Force
#>

[CmdletBinding()]
param(
    [ValidateSet('dev','test','prod')]
    [string] $Env = 'dev',

    [ValidateSet('status','updateSQL','update','history','rollbackCount','rollbackToTag','tag','clearCheckSums','changelogSync')]
    [string] $Command = 'status',

    [int]    $RollbackCount,
    [string] $RollbackTag,

    [string] $Labels,
    [string] $Contexts,

    [switch] $DryRunThenConfirm,   # if set and Command=update -> first run updateSQL and ask for confirmation
    [switch] $Force,               # skip confirmations (use with caution for prod)

    [string] $Region = "ap-south-1",
    [string] $Profile = "ApnaFundAdmin",

# only needed if 'liquibase' CLI is NOT on PATH
    [string] $LiquibaseJarPath = "$PSScriptRoot\liquibase\liquibase.jar"
)

# --- map env -> defaults file (relative path in your repo)
$DefaultsFile = switch ($Env) {
    'prod' { 'server\src\main\resources\db\liquibase.ec2-prod.properties' }
    'test' { 'server\src\main\resources\db\liquibase.ec2-test.properties' }
    default { 'server\src\main\resources\db\liquibase.ec2-dev.properties' }
}

# --- locate changelog and search-path (adjust if your repo differs)
$ChangeLogFile = 'changelog/db.changelog-master.yaml'
$SearchPath    = 'C:/Users/sunil/AndroidStudioProjects/ApnaFund/server/src/main/resources/db'

# --- logging
$LogDir = Join-Path (Get-Location).Path "logs"
if (-not (Test-Path $LogDir)) { New-Item -ItemType Directory -Force -Path $LogDir | Out-Null }
$Timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$LogFile = Join-Path $LogDir "liquibase_${Env}_${Timestamp}.log"

"=============================================================" | Out-File -FilePath $LogFile -Append
" Liquibase Execution Log - $Timestamp" | Out-File -FilePath $LogFile -Append
" Command: $Command" | Out-File -FilePath $LogFile -Append
" Environment: $Env" | Out-File -FilePath $LogFile -Append
"=============================================================" | Out-File -FilePath $LogFile -Append

# --- fetch DB password from Secrets Manager
$SecretId = "apnafund/$Env/postgres"
Write-Host "Fetching DB credentials for $Env from Secrets Manager..." -ForegroundColor Cyan
$secretJson = aws secretsmanager get-secret-value `
  --secret-id $SecretId `
  --region $Region --profile $Profile `
  --query SecretString --output text 2>$null

if (-not $secretJson) {
    Write-Host "Failed to fetch secret for $Env" -ForegroundColor Red
    exit 2
}

try { $secret = $secretJson | ConvertFrom-Json } catch {
    Write-Host "Failed to parse secret JSON: $_" -ForegroundColor Red
    exit 3
}

$LB_PG_PASS = $secret.deploy_pass
$LB_PG_USER = if ($secret.PSObject.Properties.Name -contains "deploy_user") { $secret.deploy_user } else { $null }
$LB_PG_DB   = if ($secret.PSObject.Properties.Name -contains "deploy_db")   { $secret.deploy_db }   else { $null }

if (-not $LB_PG_PASS) {
    Write-Host "Missing 'deploy_pass' in secret JSON for $Env" -ForegroundColor Red
    exit 4
}
Write-Host "Secret fetched successfully for $Env." -ForegroundColor Green

# --- detect whether 'liquibase' CLI is available
$liquibaseCmd = $null
try {
    $lc = Get-Command liquibase -ErrorAction SilentlyContinue
    if ($lc) { $liquibaseCmd = "liquibase" }
} catch {}
# if not found, check for jar
$useJar = $false
if (-not $liquibaseCmd) {
    if (-not (Test-Path $LiquibaseJarPath)) {
        Write-Host "Liquibase CLI not found on PATH and JAR not found at $LiquibaseJarPath" -ForegroundColor Red
        Write-Host "Either install Liquibase CLI or provide -LiquibaseJarPath pointing to liquibase.jar" -ForegroundColor Yellow
        exit 5
    } else {
        $useJar = $true
        Write-Host "Liquibase CLI not on PATH. Will use java -jar $LiquibaseJarPath" -ForegroundColor Yellow
        # check java
        try { & java -version 2>$null } catch {
            Write-Host "Java is required when using liquibase.jar but 'java' is not available on PATH." -ForegroundColor Red
            exit 6
        }
    }
}

# --- build base args array used for all commands (we will construct final command per action)
$baseArgs = @()
$baseArgs += "--defaultsFile=$DefaultsFile"
$baseArgs += "--changeLogFile=$ChangeLogFile"
$baseArgs += "--search-path=$SearchPath"
$baseArgs += "--log-level=debug"

if ($Labels) { $baseArgs += "--labels=$Labels" }
if ($Contexts) { $baseArgs += "--contexts=$Contexts" }

# allow secret overrides
if ($LB_PG_USER) { $baseArgs += "--username=$LB_PG_USER" }
if ($LB_PG_DB)   { $baseArgs += "--url=jdbc:postgresql://127.0.0.1:5432/$LB_PG_DB" }

# password always supplied via --password
$baseArgs += "--password=$LB_PG_PASS"

# helper to build final command string or arg array
function Build-CommandArgs([string]$action) {
    $args = @($baseArgs)
    switch ($action) {
        "status"         { $args += "status" }
        "updateSQL"      { $args += "updateSQL" }
        "update"         { $args += "update" }
        "history"        { $args += "history" }
        "clearCheckSums" { $args += "clearCheckSums" }
        "changelogSync"  { $args += "changelogSync" }
        "tag"            {
            if (-not $RollbackTag) { throw "Please provide -RollbackTag for tag." }
            $args += "tag"
            $args += $RollbackTag
        }
        "rollbackToTag"  {
            if (-not $RollbackTag) { throw "Please provide -RollbackTag for rollbackToTag." }
            $args += "rollbackToTag"
            $args += $RollbackTag
        }
        "rollbackCount"  {
            if (-not $RollbackCount) { throw "Please provide -RollbackCount for rollbackCount." }
            $args += "rollbackCount"
            $args += $RollbackCount
        }
        default { throw "Unsupported action $action" }
    }
    return $args
}

# --- Safety: prompt for prod destructive ops
$destructive = @('update','rollbackCount','rollbackToTag')
if ($Env -eq 'prod' -and $destructive -contains $Command -and -not $Force) {
    $confirm = Read-Host "You are about to run '$Command' on PROD. Type 'YES' to continue"
    if ($confirm -ne 'YES') {
        Write-Host "Aborting." -ForegroundColor Yellow
        exit 0
    }
}

# --- If DryRunThenConfirm and Command is update -> run updateSQL first and show user then ask confirmation
if ($DryRunThenConfirm -and $Command -eq 'update') {
    Write-Host "Running updateSQL (dry-run) to show SQL..." -ForegroundColor Cyan
    $args = Build-CommandArgs "updateSQL"
    if ($useJar) {
        $procArgs = @("-jar", $LiquibaseJarPath) + $args
        Write-Host "java $($procArgs -join ' ')" | Out-File -FilePath $LogFile -Append
        & java @procArgs 2>&1 | Tee-Object -FilePath $LogFile -Append
        $code = $LASTEXITCODE
    } else {
        $cmdLine = "liquibase " + ($args -join ' ')
        Write-Host $cmdLine | Out-File -FilePath $LogFile -Append
        cmd /c $cmdLine 2>&1 | Tee-Object -FilePath $LogFile -Append
        $code = $LASTEXITCODE
    }

    if ($code -ne 0) {
        Write-Host "updateSQL failed (exit $code). See $LogFile" -ForegroundColor Red
        exit $code
    }

    Write-Host ""
    $ans = Read-Host "If the SQL above looks good, type 'RUN' to proceed with actual update"
    if ($ans -ne 'RUN') {
        Write-Host "Aborting actual update." -ForegroundColor Yellow
        exit 0
    }
    # set to perform actual update
    $Command = 'update'
}

# --- execute the requested command
try {
    $args = Build-CommandArgs $Command
} catch {
    Write-Host "Argument building failed: $_" -ForegroundColor Red
    exit 7
}

"Command to run: $Command" | Out-File -FilePath $LogFile -Append
"Args: $($args -join ' ')" | Out-File -FilePath $LogFile -Append

Write-Host "`n▶ Running Liquibase command: $Command" -ForegroundColor Cyan
if ($useJar) {
    $procArgs = @("-jar", $LiquibaseJarPath) + $args
    Write-Host "java $($procArgs -join ' ')" -ForegroundColor Gray
    & java @procArgs 2>&1 | Tee-Object -FilePath $LogFile -Append
    $exitCode = $LASTEXITCODE
} else {
    $cmdLine = "liquibase " + ($args -join ' ')
    Write-Host $cmdLine -ForegroundColor Gray
    cmd /c $cmdLine 2>&1 | Tee-Object -FilePath $LogFile -Append
    $exitCode = $LASTEXITCODE
}

"Exit code: $exitCode" | Out-File -FilePath $LogFile -Append
if ($exitCode -eq 0) {
    Write-Host "Liquibase command '$Command' completed successfully. Log: $LogFile" -ForegroundColor Green
} else {
    Write-Host "Liquibase command '$Command' FAILED (exit $exitCode). See log: $LogFile" -ForegroundColor Red
}

exit $exitCode
