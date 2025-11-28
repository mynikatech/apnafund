param(
    [string]$Env = "dev",
    [string]$Region = "ap-south-1",
    [string]$Profile = "ApnaFundAdmin"
)

$Bucket = "apnafund-config-861082243595-$Region"
$Key    = "apnafund/$Env/startup.sh"
$Local  = ".\startup.sh"

if (-not (Test-Path $Local)) {
    Write-Error "Local file $Local not found!"
    exit 1
}

Write-Host "Uploading $Local to s3://$Bucket/$Key..." -ForegroundColor Cyan
aws s3 cp $Local "s3://$Bucket/$Key" --region $Region --profile $Profile --cache-control "no-cache" --content-type "text/x-shellscript"

# Fetch ETag + Version
$meta = aws s3api head-object --bucket $Bucket --key $Key --region $Region --profile $Profile | ConvertFrom-Json
$etag = $meta.ETag.Trim('"')
$version = $meta.VersionId

Write-Host "Upload complete!"
Write-Host "ETag:     $etag"
Write-Host "Version:  $version"
Write-Host "Timestamp:" (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
