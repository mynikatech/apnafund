<#
  Unified Liquibase runner for ApnaBank (baseline + future updates)
  Examples:
    .\run_liquibase.ps1 -Env test -Command update -Labels baseline
    .\run_liquibase.ps1 -Env test -Command update -Labels "!baseline"
    .\run_liquibase.ps1 -Env prod -Command update
    .\run_liquibase.ps1 -Env test -Command updateSQL -Labels "!baseline"
    .\run_liquibase.ps1 -Env test -Command rollbackCount -RollbackCount 2
    .\run_liquibase.ps1 -Env test -Command tag -RollbackTag baseline-1.0
#>

[CmdletBinding()]
param(
    [ValidateSet('dev','test','prod')]
    [string] $Env = 'dev',

    [ValidateSet('status','updateSQL','update','history','rollbackCount','rollbackToTag','tag','clearCheckSums','changelogSync')]
    [string] $Command = 'status',

    [int]    $RollbackCount,
    [string] $RollbackTag,

# Optional targeting
    [string] $Labels,
    [string] $Contexts
)

# --- 1) Map env -> defaults file
$DefaultsFile = switch ($Env) {
    'prod' { 'server\src\main\resources\db\liquibase.prod.properties' }
    'test' { 'server\src\main\resources\db\liquibase.test.properties' }
    default { 'server\src\main\resources\db\liquibase.dev.properties' }
}

# --- 2) Paths
# Use one master changelog that includes baseline/ and changes/
$ChangeLogFile = 'changelog/db.changelog-master.yaml'
$SearchPath    = 'C:/Users/sunil/AndroidStudioProjects/ApnaFund/server/src/main/resources/db'

# --- 3) Logging
$LogDir = Join-Path (Get-Location).Path "logs"
if (-not (Test-Path $LogDir)) { New-Item -ItemType Directory -Force -Path $LogDir | Out-Null }
$Timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$LogFile = Join-Path $LogDir "liquibase_${Env}.log"

"=============================================================" | Out-File -FilePath $LogFile -Append
" Liquibase Execution Log - $Timestamp" | Out-File -FilePath $LogFile -Append
"=============================================================" | Out-File -FilePath $LogFile -Append

# --- 4) Build args
$ArgsList = @(
    "liquibase",
    "--defaultsFile=$DefaultsFile",
    "--changelog-file=$ChangeLogFile",
    "--search-path=$SearchPath",
    "--log-level=debug"
)

if ($Labels)   { $ArgsList += "--labels=$Labels" }
if ($Contexts) { $ArgsList += "--contexts=$Contexts" }

$ArgsList += $Command

if ($Command -eq 'rollbackCount') {
    if (-not $RollbackCount) { throw "Please provide -RollbackCount <N> for rollbackCount." }
    $ArgsList += $RollbackCount
}

if ($Command -eq 'rollbackToTag') {
    if (-not $RollbackTag) { throw "Please provide -RollbackTag <tag> for rollbackToTag." }
    $ArgsList += $RollbackTag
}

if ($Command -eq 'tag') {
    if (-not $RollbackTag) { throw "Please provide -RollbackTag <tag> for tag." }
    $ArgsList += $RollbackTag
}

# --- 5) Execute and tee logs
$CommandLine = $ArgsList -join ' '
Write-Host "`n▶ Running Liquibase" -ForegroundColor Cyan
Write-Host $CommandLine
Write-Host ""

"Command Line: $CommandLine" | Out-File -FilePath $LogFile -Append
"Working Directory: $((Get-Location).Path)" | Out-File -FilePath $LogFile -Append
"-------------------------------------------------------------" | Out-File -FilePath $LogFile -Append

$StartTime = Get-Date
cmd /c $CommandLine 2>&1 | Tee-Object -FilePath $LogFile -Append
$EndTime = Get-Date

"-------------------------------------------------------------" | Out-File -FilePath $LogFile -Append
"Execution Completed: $(Get-Date)" | Out-File -FilePath $LogFile -Append
"Duration: $((New-TimeSpan -Start $StartTime -End $EndTime).ToString())" | Out-File -FilePath $LogFile -Append
"=============================================================" | Out-File -FilePath $LogFile -Append

Write-Host "`n✅ Log saved to: $LogFile" -ForegroundColor Green
