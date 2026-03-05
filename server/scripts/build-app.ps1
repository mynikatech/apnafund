param(
    [string]$Env = "dev"
)

Write-Host "========================================"
Write-Host " ApnaFund Server Build"
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
java -version

# ------------------------------------------------------------
# MOVE TO PROJECT ROOT (IMPORTANT)
# ------------------------------------------------------------
$projectRoot = Resolve-Path "$PSScriptRoot\..\.."
Write-Host "Changing directory to project root: $projectRoot"
Set-Location $projectRoot

# ------------------------------------------------------------
# Run Gradle
# ------------------------------------------------------------
if (-not (Test-Path ".\gradlew.bat")) {
    Write-Error "gradlew.bat not found in project root"
    exit 1
}

Write-Host "Running Gradle build..."
.\gradlew.bat :server:clean :server:shadowJar --no-daemon --console=plain

if ($LASTEXITCODE -ne 0) {
    ?  d
    Write-Error "Gradle build failed"
    exit 1
}

Write-Host "Gradle build successful"
