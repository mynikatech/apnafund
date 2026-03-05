param(
    [string]$Env = "dev"
)

Write-Host "========================================"
Write-Host " ApnaFund Lambda Email Processor Build"
Write-Host " ENV : $Env"
Write-Host "========================================"

# ------------------------------------------------------------
# Ensure JAVA_HOME
# ------------------------------------------------------------
if (-not $env:JAVA_HOME -or $env:JAVA_HOME -eq "") {
    $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
}

Write-Host "JAVA_HOME = $env:JAVA_HOME"
& "$env:JAVA_HOME\bin\java.exe" -version

# ------------------------------------------------------------
# Resolve paths WITHOUT changing directory
# ------------------------------------------------------------
$scriptDir   = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Resolve-Path "$scriptDir\..\.."

$gradlew = Join-Path $projectRoot "gradlew.bat"

if (-not (Test-Path $gradlew)) {
    Write-Error "gradlew.bat not found at $gradlew"
    exit 1
}

Write-Host "Project Root : $projectRoot"
Write-Host "Scripts Dir  : $scriptDir"
Write-Host "Gradle       : $gradlew"

# ------------------------------------------------------------
# Run Gradle (explicit project-dir)
# ------------------------------------------------------------
Write-Host "Running Lambda build..."

& $gradlew `
    --project-dir $projectRoot `
    :lambda:whatsapp-processor:clean `
    :lambda:whatsapp-processor:build `
    --no-daemon `
    --console=plain

if ($LASTEXITCODE -ne 0) {
    Write-Error "Lambda build failed"
    exit 1
}

Write-Host "Lambda build successful"

$artifactSrc = "$projectRoot\lambda\whatsapp-processor\build\libs\whatsapp-processor.jar"
$artifactDstDir = "$projectRoot\infra\artifacts"
$artifactDst = "$artifactDstDir\whatsapp-processor.jar"

if (-not (Test-Path $artifactDstDir)) {
    New-Item -ItemType Directory -Path $artifactDstDir | Out-Null
}

Copy-Item -Path $artifactSrc -Destination $artifactDst -Force
Write-Host "Copied Lambda artifact to infra/artifacts"
