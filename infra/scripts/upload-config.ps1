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

$StartupLocal       = Join-Path $EnvFolder "startup.sh"
$DiskSetupLocal     = Join-Path $EnvFolder "apnafund-disk-setup.sh"
$UpdaterLocal       = Join-Path $EnvFolder "update_startup_from_s3.sh"
$StartServiceLocal  = Join-Path $EnvFolder "apnafund-startup.service"

# ---- S3 destination (env-scoped) ----
$Bucket = "apnafund-config-861082243595-$Region"
$StartupKey = "apnafund/$Env/startup.sh"
$DiskSetupKey = "apnafund/$Env/apnafund-disk-setup.sh"
$UpdaterSetUpKey = "apnafund/$Env/update_startup_from_s3.sh"
$StartServiceKey = "apnafund/$Env/apnafund-startup.service"

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
Assert-File $DiskSetupLocal
Assert-File $UpdaterLocal
Assert-File $StartServiceLocal

# ---- Upload all ----
Upload-And-Report -local $StartupLocal    -bucket $Bucket -key $StartupKey   -region $Region -profile $Profile -contentType "text/x-shellscript"
Upload-And-Report -local $DiskSetupLocal  -bucket $Bucket -key $DiskSetupKey -region $Region -profile $Profile -contentType "text/x-shellscript"
Upload-And-Report -local $UpdaterLocal    -bucket $Bucket -key $UpdaterSetUpKey -region $Region -profile $Profile -contentType "text/x-shellscript"
Upload-And-Report -local $StartServiceLocal    -bucket $Bucket -key $StartServiceKey -region $Region -profile $Profile -contentType "text/x-shellscript"

