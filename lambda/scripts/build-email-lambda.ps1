param (
    [string]$Env = "dev"
)

$ErrorActionPreference = "Stop"

Write-Host "=== Building Email Processor Lambda ===" -ForegroundColor Cyan

# Paths
$projectRoot = Resolve-Path "$PSScriptRoot\..\.."
$lambdaDir   = Join-Path $projectRoot "lambda\email-processor"
$outputJar   = Join-Path $lambdaDir "build\libs\email-processor.jar"
$artifactDir = Join-Path $projectRoot "infra\envs\$Env\artifacts"
$artifactJar = Join-Path $artifactDir "email-processor.jar"

Write-Host "Lambda project: $lambdaDir"
Write-Host "Target env: $Env"

# Validate lambda project
if (-not (Test-Path $lambdaDir)) {
    throw "Lambda project directory not found: $lambdaDir"
}

# Build Lambda
Push-Location $lambdaDir
try {
    Write-Host "Running Gradle shadowJar..."

    if (Test-Path ".\gradlew.bat") {
        .\gradlew.bat clean shadowJar
    }
    elseif (Test-Path "./gradlew") {
        ./gradlew clean shadowJar
    }
    else {
        throw "Gradle wrapper not found (gradlew / gradlew.bat)"
    }
}
finally {
    Pop-Location
}

# Validate JAR
if (-not (Test-Path $outputJar)) {
    throw "Expected JAR not found: $outputJar"
}

# Ensure artifacts directory
if (-not (Test-Path $artifactDir)) {
    Write-Host "Creating artifacts directory: $artifactDir"
    New-Item -ItemType Directory -Path $artifactDir | Out-Null
}

# Copy JAR
Copy-Item -Force $outputJar $artifactJar

Write-Host "✔ Email processor JAR built successfully"
Write-Host "✔ Copied to: $artifactJar" -ForegroundColor Green
