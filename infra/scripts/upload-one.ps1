param(
    [Parameter(Mandatory=$true)]
    [string]$Env,

    [Parameter(Mandatory=$true)]
    [string]$File,   # relative or absolute path OK

    [string]$Bucket = "apnafund-config-861082243595-ap-south-1"
)

# Resolve full path
$FullPath = Resolve-Path $File -ErrorAction Stop
$FileName = Split-Path $FullPath -Leaf

# ALWAYS flatten structure in S3
$S3Key = "apnafund/$Env/$FileName"

Write-Host "[INFO] Uploading $FullPath -> s3://$Bucket/$S3Key"

aws s3 cp $FullPath "s3://$Bucket/$S3Key" --only-show-errors

if ($LASTEXITCODE -eq 0) {
    Write-Host "[SUCCESS] Uploaded $FileName to env=$Env"
} else {
    Write-Host "[ERROR] Upload failed."
}
