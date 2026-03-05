param(
    [string]$Env = "dev",
    [string]$Region = "ap-south-1",
    [string]$Profile = "jenkins-bot"
)

# ----------------------------
# LOCAL FILE LAYOUT (inside server repo)
# server/
#   service/
#       apnafund.conf           (rendered nginx site config)
#       apnafund.service        (systemd unit)
#       env-dev.properties      (rendered env)
#   build/
#       apnafund-server.jar     (CI built artifact)
#   scripts/
#       app-setup.sh
#       deploy-app.sh
#       deploy-nginx.sh
#       sync-app-scripts-from-s3.sh
# ----------------------------

$RepoRoot       = Split-Path -Parent $PSScriptRoot
$InfraFolder  = Join-Path $RepoRoot "infra"
$BuildFolder    = Join-Path $RepoRoot "build/lib"
$ScriptsFolder  = Join-Path $RepoRoot "scripts"
$EnvFolder      = Join-Path $RepoRoot "env"

# --- Local files ---
$NginxConfLocal       = Join-Path $InfraFolder "apnafund.conf"
$ServiceFileLocal     = Join-Path $InfraFolder "apnafund.service"
$AppSetupLocal        = Join-Path $ScriptsFolder "app-setup.sh"
$DeployAppLocal       = Join-Path $ScriptsFolder "deploy-app.sh"
$DeployNginxLocal     = Join-Path $ScriptsFolder "deploy-nginx.sh"
$SyncScriptsLocal     = Join-Path $ScriptsFolder "sync-app-scripts-from-s3.sh"
$GenerateCertLocal     = Join-Path $ScriptsFolder "generate-cert.sh"
$FirebaseCredsLocal = Join-Path $RepoRoot "secure/firebase/firebase-service-account.json"
# --- S3 BUCKETS (env-specific where applicable) ---
$AppBucket    = "apnafund-app-861082243595-$Region"
$ConfigBucket = "apnafund-config-861082243595-$Region"

# Root S3 prefix
$RootPrefix = "apnafund/$Env"

# --- S3 KEYS ---
$ServiceKey        = "$RootPrefix/app/apnafund.service"

$ScriptsPrefix     = "$RootPrefix/config"
$AppSetupKey       = "$ScriptsPrefix/app-setup.sh"
$DeployAppKey      = "$ScriptsPrefix/deploy-app.sh"
$DeployNginxKey    = "$ScriptsPrefix/deploy-nginx.sh"
$SyncScriptsKey    = "$ScriptsPrefix/sync-app-scripts-from-s3.sh"
$GenerateCertKey    = "$ScriptsPrefix/generate-cert.sh"
$FirebaseCredsKey = "$RootPrefix/config/firebase/firebase-service-account.json"

$NginxConfKey      = "$ScriptsPrefix/nginx/apnafund.conf"

function Assert-File($path) {
    if (-not (Test-Path $path)) {
        Write-Error "Local file missing: $path"
        exit 1
    }
}

function Upload-And-Report($local, $bucket, $key, $region, $Profile, $contentType) {
    Write-Host "Uploading $local → s3://$bucket/$key" -ForegroundColor Cyan
    aws s3 cp $local "s3://$bucket/$key" `
        --region $region `
        --profile $profile `
        --cache-control "no-cache" `
        --content-type $contentType | Out-Null

    $meta = aws s3api head-object --bucket $bucket --key $key --region $region --profile $profile | ConvertFrom-Json

    # PowerShell 5 compatible null-safe handling
    $etag = if ($meta.ETag) { $meta.ETag.Trim('"') } else { "(none)" }
    $version = if ($meta.VersionId) { $meta.VersionId } else { "(no versioning)" }

    Write-Host ("  ETag    : {0}" -f $etag)
    Write-Host ("  Version : {0}" -f $version)
    Write-Host ("  Updated : {0}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
    Write-Host ""
}

# ---- Pre-checks: ensure required local files exist ----
Assert-File $NginxConfLocal
Assert-File $ServiceFileLocal
Assert-File $AppSetupLocal
Assert-File $DeployAppLocal
Assert-File $DeployNginxLocal
Assert-File $SyncScriptsLocal
Assert-File $GenerateCertLocal
Assert-File $FirebaseCredsLocal

# ---- Upload: app artifacts & env ----
Upload-And-Report -local $ServiceFileLocal  -bucket $AppBucket   -key $ServiceKey   -region $Region -profile $Profile -contentType "text/plain"

# ---- Upload: scripts (to app bucket under scripts/) ----
Upload-And-Report -local $AppSetupLocal     -bucket $AppBucket   -key $AppSetupKey    -region $Region -profile $Profile -contentType "text/x-shellscript"
Upload-And-Report -local $DeployAppLocal    -bucket $AppBucket   -key $DeployAppKey   -region $Region -profile $Profile -contentType "text/x-shellscript"
Upload-And-Report -local $DeployNginxLocal  -bucket $AppBucket   -key $DeployNginxKey -region $Region -profile $Profile -contentType "text/x-shellscript"
Upload-And-Report -local $SyncScriptsLocal  -bucket $AppBucket   -key $SyncScriptsKey -region $Region -profile $Profile -contentType "text/x-shellscript"
Upload-And-Report -local $GenerateCertLocal  -bucket $AppBucket   -key $GenerateCertKey -region $Region -profile $Profile -contentType "text/x-shellscript"
# ---- Upload: nginx site config to config bucket ----
Upload-And-Report -local $NginxConfLocal    -bucket $ConfigBucket -key $NginxConfKey  -region $Region -profile $Profile -contentType "text/plain"
Upload-And-Report -local $FirebaseCredsLocal  -bucket $ConfigBucket   -key $FirebaseCredsKey  -region $Region -profile $Profile -contentType "application/json"

Write-Host "=== App config & scripts upload completed successfully ===" -ForegroundColor Green
