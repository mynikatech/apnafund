<#
Minimal Liquibase runner (robust repo detection, BOM-free temp props, case-insensitive param cleanup)
Usage:
  .\run-liquibase-ec2-wdocker.ps1 -Env dev -Command updateSQL -Labels "baselineSchema" -AwsProfile "ApnaFundAdmin"
#>

[CmdletBinding()]
param(
    [ValidateSet('dev','test','prod')] [string] $Env = 'dev',
    [ValidateSet('status','updateSQL','update','history','rollbackCount','rollbackToTag','tag','clearCheckSums','changelogSync')] [string] $Command = 'status',
    [string] $Labels,
    [string] $SecretName = "apnafund/dev/postgres",
    [string] $Region     = "ap-south-1",
    [string] $AwsProfile,
    [string] $DefaultsFile,  # optional override (relative to repo root or absolute),
    [string] $ChangeLogFile = "changelog/db.changelog-ec2master.yaml"
)

Set-StrictMode -Version Latest

# -------------------------
# Helper: walk upward from script directory to find repo root containing server/src/main/resources/db
function Find-RepoRoot([int]$maxLevels = 6) {
    $cur = $PSScriptRoot
    for ($i = 0; $i -le $maxLevels; $i++) {
        if (-not $cur) { break }
        $check = Join-Path $cur "server\src\main\resources\db"
        if (Test-Path $check) { return $cur }
        $parent = Split-Path $cur -Parent
        if (-not $parent -or $parent -eq $cur) { break }
        $cur = $parent
    }
    return $null
}

# Determine DefaultsFile (override allowed)
$RepoRoot = $null
if (-not $DefaultsFile) {
    $RepoRoot = Find-RepoRoot 6
    if ($RepoRoot) {
        $isAi = $SecretName -like "*postgres-ai*"
        $defaultsDir = Join-Path $RepoRoot "server\src\main\resources\db"
        if ($isAi) {
            $adminDefaults  = Join-Path $defaultsDir "liquibase.ec2-$Env-ai-admin.properties"
            $deployDefaults = Join-Path $defaultsDir "liquibase.ec2-$Env-ai-deploy.properties"
        }
        else {
            $adminDefaults  = Join-Path $defaultsDir "liquibase.ec2-$Env-admin.properties"
            $deployDefaults = Join-Path $defaultsDir "liquibase.ec2-$Env-deploy.properties"
        }
        $genericDefaults= Join-Path $defaultsDir "liquibase.ec2-$Env.properties"
        $legacyDefaults = Join-Path $defaultsDir "liquibase.$Env.properties"

        if ($Labels -and $Labels -match 'baselineSchema' -and (Test-Path $adminDefaults)) { $DefaultsFile = $adminDefaults }
        elseif ($Labels -and $Labels -match 'baselineObjects' -and (Test-Path $deployDefaults)) { $DefaultsFile = $deployDefaults }
        else {
            foreach ($p in @($genericDefaults, $deployDefaults, $adminDefaults, $legacyDefaults)) { if (Test-Path $p) { $DefaultsFile = $p; break } }
        }
    }
}

if (-not $DefaultsFile) {
    Write-Error "Could not find defaults file. Provide -DefaultsFile or ensure 'server/src/main/resources/db' exists in an ancestor directory."
    exit 2
}
# if relative provided, make it absolute relative to repo root (if found)
if (-not ([System.IO.Path]::IsPathRooted($DefaultsFile)) -and $RepoRoot) { $DefaultsFile = Join-Path $RepoRoot $DefaultsFile }

# --- logging setup
$LogDir = Join-Path $PSScriptRoot "logs"
if (-not (Test-Path $LogDir)) { New-Item -Path $LogDir -ItemType Directory -Force | Out-Null }
$LogFile = Join-Path $LogDir ("liquibase_{0}_{1}.log" -f $Env, (Get-Date -Format "yyyy-MM-dd_HH-mm-ss"))
"Start: $(Get-Date)" | Out-File -FilePath $LogFile -Append
"Using defaultsFile: $DefaultsFile" | Out-File -FilePath $LogFile -Append

# --- fetch secret
try {
    $awsCmd = "aws secretsmanager get-secret-value --secret-id `"$SecretName`" --query SecretString --output text --region $Region"
    if ($AwsProfile) { $awsCmd += " --profile `"$AwsProfile`"" }
    Write-Host "Fetching secret $SecretName (profile: $AwsProfile)" -ForegroundColor Cyan
    $secretJsonText = Invoke-Expression $awsCmd 2>&1
    if ($LASTEXITCODE -ne 0 -or -not $secretJsonText) { $secretJsonText | Out-File -FilePath $LogFile -Append; throw "Failed to fetch secret" }
    $secretObj = $secretJsonText | ConvertFrom-Json
} catch {
    $_ | Out-String | Out-File -FilePath $LogFile -Append
    Write-Error "Failed to fetch secret. See $LogFile"
    exit 4
}

# --- pick credentials (admin for baselineSchema, fallback deploy/admin/username)
$chosenUser = $null; $chosenPass = $null
if ($Labels -and $Labels -match 'baselineSchema' -and $secretObj.admin_user -and $secretObj.admin_pass) {
    $chosenUser = $secretObj.admin_user; $chosenPass = $secretObj.admin_pass
}
if (-not $chosenUser) {
    if ($secretObj.deploy_user -and $secretObj.deploy_pass) { $chosenUser=$secretObj.deploy_user; $chosenPass=$secretObj.deploy_pass }
    elseif ($secretObj.admin_user -and $secretObj.admin_pass) { $chosenUser=$secretObj.admin_user; $chosenPass=$secretObj.admin_pass }
    elseif ($secretObj.username -and $secretObj.password) { $chosenUser=$secretObj.username; $chosenPass=$secretObj.password }
    else { "No valid creds in secret" | Out-File -FilePath $LogFile -Append; Write-Error "No DB credentials found"; exit 6 }
}

# --- create BOM-free temp defaults file; remove ANY parameter.* lines (case-insensitive) from base
$tempDefaultsFile = Join-Path $env:TEMP ("liquibase_temp_{0}.properties" -f ([guid]::NewGuid().ToString()))
try {
    $props = @()

    if (Test-Path $DefaultsFile) {
        $base = Get-Content -Path $DefaultsFile -ErrorAction Stop
        # remove any parameter.* lines (case-insensitive) and also any leading/trailing whitespace
        $filtered = $base | Where-Object { -not ($_ -match '^(?i)\s*parameter\.') }
        $props += $filtered
    } else {
        $props += "# generated temporary defaults"
    }

    $props += ""
    $props += "# Injected parameter (temporary)"
    if ($secretObj.PSObject.Properties.Name -contains 'app_role')      { $props += "parameter.app_role=$($secretObj.app_role)" }
    if ($secretObj.PSObject.Properties.Name -contains 'app_pass')      { $props += "parameter.app_pass=$($secretObj.app_pass)" }
    if ($secretObj.PSObject.Properties.Name -contains 'deploy_pass')      { $props += "parameter.deploy_pass=$($secretObj.deploy_pass)" }
    if ($secretObj.PSObject.Properties.Name -contains 'app_user')      { $props += "parameter.app_user=$($secretObj.app_user)" }
    if ($secretObj.PSObject.Properties.Name -contains 'deploy_user')   { $props += "parameter.deploy_user=$($secretObj.deploy_user)" }
    if ($secretObj.PSObject.Properties.Name -contains 'target_schema') { $props += "parameter.target_schema=$($secretObj.target_schema.ToLower())" }

    $content = ($props -join "`n") + "`n"
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($tempDefaultsFile, $content, $utf8NoBom)

    # best-effort tighten ACLs
    #try { icacls $tempDefaultsFile /inheritance:r /grant:r "$($env:USERNAME):(R,W)" | Out-Null } catch {}

    "Temp defaults file: $tempDefaultsFile" | Out-File -FilePath $LogFile -Append
    Get-Content -Path $tempDefaultsFile -ErrorAction Stop | Select-Object -First 40 | ForEach-Object { $_ | Out-File -FilePath $LogFile -Append }
} catch {
    $_ | Out-String | Out-File -FilePath $LogFile -Append
    Write-Error "Could not create temp defaults file"
    exit 7
}

# --- Build liquibase args and run
$args = @("--defaultsFile=$tempDefaultsFile", "--changelog-file=$ChangeLogFile", "--search-path=$(Split-Path $DefaultsFile -Parent)", "--log-level=info")
if ($Labels) { $args += "--labels=$Labels" }
if ($Command) { $args += $Command }

# add connection creds for liquibase
$args += "--username=$chosenUser"
$args += "--password=$chosenPass"

$env:PATH = "C:\Program Files\liquibase-pro;$env:PATH"

# masked log
$masked = '*' * ($chosenPass.Length)
$maskedArgs = $args | ForEach-Object { if ($_ -like '--password=*') { "--password=$masked" } else { $_ } }
"Running: liquibase $($maskedArgs -join ' ')" | Out-File -FilePath $LogFile -Append
Write-Host "Running liquibase (defaults: $tempDefaultsFile) as $chosenUser" -ForegroundColor Cyan

$exitCode = 0
try {
    & liquibase @args 2>&1 | Tee-Object -FilePath $LogFile -Append
    $exitCode = $LASTEXITCODE
} catch {
    $_ | Out-String | Out-File -FilePath $LogFile -Append
    $exitCode = 3
}

# cleanup
if (Test-Path $tempDefaultsFile) { Remove-Item $tempDefaultsFile -Force -ErrorAction SilentlyContinue }
"Finished: $(Get-Date) exitCode=$exitCode" | Out-File -FilePath $LogFile -Append

if ($exitCode -eq 0) { Write-Host "Done. Log: $LogFile" -ForegroundColor Green; exit 0 }
else { Write-Host "Failed. See log: $LogFile" -ForegroundColor Red; exit $exitCode }
