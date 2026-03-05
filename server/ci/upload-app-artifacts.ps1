param(
    [string]$Env = "dev",
    [string]$Region = "ap-south-1",
    [string]$Profile = "jenkins-bot"
)

# ----------------------------
# LOCAL FILE LAYOUT
# server/
#   build/libs/apnafund-server.jar
#   env/env-dev.properties
# ----------------------------

$RepoRoot    = Split-Path -Parent $PSScriptRoot
$BuildFolder = Join-Path $RepoRoot "build/libs"
$EnvFolder   = Join-Path $RepoRoot "env"

# --- Local files ---
$EnvPropsLocal = Join-Path $EnvFolder "env-$Env.properties"
$JarLocal      = Join-Path $BuildFolder "apnafund-server.jar"

# --- S3 BUCKET ---
$AppBucket = "apnafund-app-861082243595-$Region"
$RootPrefix = "apnafund/$Env"

# --- S3 KEYS ---
$EnvPropsKey = "$RootPrefix/app/env-$Env.properties"
$JarKey      = "$RootPrefix/app/apnafund-server.jar"

function Assert-File($path) {
    if (-not (Test-Path $path)) {
        Write-Error "Local file missing: $path"
        exit 1
    }
}

function Normalize-EnvFile {
    param([string]$Path)

    Write-Host "Normalizing env file: $Path" -ForegroundColor Yellow

    # Read raw, normalize LF, UTF-8 no BOM
    $content = Get-Content $Path -Raw
    $content = $content -replace "`r`n", "`n"
    $content = $content -replace "`r", "`n"

    # Hard fail if line continuation exists
    if ($content -match "\\\n") {
        Write-Error "ERROR: Line continuation (\\) detected in env file. Fix the file before upload."
        exit 1
    }

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $content, $utf8NoBom)
}

function Upload-And-Report {
    param(
        [string]$Local,
        [string]$Bucket,
        [string]$Key,
        [string]$Region,
        [string]$Profile,
        [string]$ContentType
    )

    Write-Host "Uploading $Local → s3://$Bucket/$Key" -ForegroundColor Cyan

    aws s3 cp `
        "$Local" `
        "s3://$Bucket/$Key" `
        --region $Region `
        --profile $Profile `
        --cache-control "no-cache" `
        --content-type $ContentType `
        --no-progress | Out-Null

    $meta = aws s3api head-object `
        --bucket $Bucket `
        --key $Key `
        --region $Region `
        --profile $Profile | ConvertFrom-Json

    Write-Host ("  ETag    : {0}" -f $meta.ETag.Trim('"'))
    Write-Host ("  Updated : {0}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
    Write-Host ""
}

# ---- Pre-checks ----
Assert-File $EnvPropsLocal
Assert-File $JarLocal

# ---- Normalize env file BEFORE upload ----
Normalize-EnvFile $EnvPropsLocal

Write-Host "=== Uploading ApnaFund app artifacts to S3 ===" -ForegroundColor Yellow

Upload-And-Report `
    -Local $EnvPropsLocal `
    -Bucket $AppBucket `
    -Key $EnvPropsKey `
    -Region $Region `
    -Profile $Profile `
    -ContentType "text/plain"

Upload-And-Report `
    -Local $JarLocal `
    -Bucket $AppBucket `
    -Key $JarKey `
    -Region $Region `
    -Profile $Profile `
    -ContentType "application/java-archive"

Write-Host "=== App artifacts upload completed successfully ===" -ForegroundColor Green
