param(
    [string]$Env = "dev",
    [string]$Region = "ap-south-1",
    [string]$Profile = "jenkins-bot"
)

# ---- Local files (env-scoped) ----
# Place these in your repo under: infra/scripts/dev/...
$LocalDir = Join-Path $PSScriptRoot $Env
$ScriptRoot = Split-Path -Parent $PSScriptRoot
$EnvFolder  = Join-Path $ScriptRoot "envs\$Env"
$ScriptsFolder = Join-Path $ScriptRoot "scripts"

$StartupLocal       = Join-Path $EnvFolder "startup.sh"
$ShutdownLocal       = Join-Path $EnvFolder "shutdown.sh"
$DiskSetupLocal     = Join-Path $EnvFolder "apnafund-disk-setup.sh"
$UpdaterLocal       = Join-Path $EnvFolder "sync-scripts-from-s3.sh"
$StartServiceLocal  = Join-Path $EnvFolder "apnafund-startup.service"
$NginxInstallLocal  = Join-Path $EnvFolder "nginx-install.sh"
$NginxSSLSetupLocal = Join-Path $EnvFolder "nginx-ssl-setup.sh"
$ServiceFileLocal   = Join-Path $EnvFolder "apnafund.service"
$EnvConfLocal       = Join-Path $EnvFolder "apnafund-env.conf"
$SsmRefreshLocal    = Join-Path $EnvFolder "ssm-refresh.sh"
$PostGreSqlLocal    = Join-Path $ScriptsFolder "postgres-install.sh"



# ---- S3 destination (env-scoped) ----
$Bucket = "apnafund-config-861082243595-$Region"
$StartupKey = "apnafund/$Env/startup.sh"
$ShutdownKey = "apnafund/$Env/shutdown.sh"
$DiskSetupKey = "apnafund/$Env/apnafund-disk-setup.sh"
$UpdaterSetUpKey = "apnafund/$Env/sync-scripts-from-s3.sh"
$StartServiceKey = "apnafund/$Env/apnafund-startup.service"
$NginxInstallKey  = "apnafund/$Env/nginx-install.sh"
$NginxSSLKey      = "apnafund/$Env/nginx-ssl-setup.sh"
$ServiceFileKey   = "apnafund/$Env/apnafund.service"
$EnvConfKey       = "apnafund/$Env/apnafund-env.conf"
$SsmRefreshKey       = "apnafund/$Env/ssm-refresh.sh"
$PostGreSqlKey       = "apnafund/$Env/postgres-install.sh"

function Assert-File($path) {
    if (-not (Test-Path $path)) {
        Write-Error "Local file not found: $path"
        exit 1
    }
}

function Upload-And-Report($local, $bucket, $key, $region, $profile, $contentType) {
    Write-Host "Uploading $local -> s3://$bucket/$key" -ForegroundColor Cyan
    aws s3 cp $local "s3://$bucket/$key" `
        --region $region --profile $profile `
        --cache-control "no-cache" `
        --content-type $contentType | Out-Null

    $meta = aws s3api head-object --bucket $bucket --key $key --region $region --profile $profile | ConvertFrom-Json
    $etag = $meta.ETag.Trim('"')
    $version = $meta.VersionId
    Write-Host ("  ETag    : {0}" -f $etag)
    if ($version) { Write-Host ("  Version : {0}" -f $version) } else { Write-Host "  Version : (bucket not versioned)" }
    Write-Host ("  Updated : {0}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
    Write-Host ""
}

# ---- Checks ----
Assert-File $StartupLocal
Assert-File $ShutdownLocal
Assert-File $DiskSetupLocal
Assert-File $UpdaterLocal
Assert-File $StartServiceLocal
Assert-File $NginxInstallLocal
Assert-File $NginxSSLSetupLocal
Assert-File $ServiceFileLocal
Assert-File $EnvConfLocal
Assert-File $SsmRefreshLocal
Assert-File $PostGreSqlLocal

# ---- Upload all ----
Upload-And-Report -local $StartupLocal    -bucket $Bucket -key $StartupKey   -region $Region -profile $Profile -contentType "text/x-shellscript"
Upload-And-Report -local $ShutdownLocal    -bucket $Bucket -key $ShutdownKey   -region $Region -profile $Profile -contentType "text/x-shellscript"
Upload-And-Report -local $DiskSetupLocal  -bucket $Bucket -key $DiskSetupKey -region $Region -profile $Profile -contentType "text/x-shellscript"
Upload-And-Report -local $UpdaterLocal    -bucket $Bucket -key $UpdaterSetUpKey -region $Region -profile $Profile -contentType "text/x-shellscript"
Upload-And-Report -local $StartServiceLocal -bucket $Bucket -key $StartServiceKey -region $Region -profile $Profile -contentType "text/x-shellscript"
Upload-And-Report -local $NginxInstallLocal -bucket $Bucket -key $NginxInstallKey -region $Region -profile $Profile -contentType "text/x-shellscript"
Upload-And-Report -local $NginxSSLSetupLocal -bucket $Bucket -key $NginxSSLKey -region $Region -profile $Profile -contentType "text/x-shellscript"
Upload-And-Report -local $ServiceFileLocal -bucket $Bucket -key $ServiceFileKey  -region $Region -profile $Profile -contentType "text/plain"
Upload-And-Report -local $EnvConfLocal -bucket $Bucket -key $EnvConfKey -region $Region -profile $Profile -contentType "text/plain"
Upload-And-Report -local $SsmRefreshLocal -bucket $Bucket -key $SsmRefreshKey -region $Region -profile $Profile -contentType "text/x-shellscript"
Upload-And-Report -local $PostGreSqlLocal -bucket $Bucket -key $PostGreSqlKey -region $Region -profile $Profile -contentType "text/x-shellscript"