<#  run_server.ps1
    Starts the Ktor server with env vars for DB + HTTP/HTTPS.

    Examples:
      .\run_server.ps1
      .\run_server.ps1 -Env prod -UseHttps -HttpsPort 8443 -KeystorePath .\dev-keystore.p12 -KeystoreType PKCS12 -KeyAlias local-dev -KeystorePassword changeit
      .\run_server.ps1 -ListenHost 0.0.0.0 -Port 8080 -UseHttps -HttpsPort 8443
#>

[CmdletBinding()]
param(
# Server bind / ports
    [string] $ListenHost  = "0.0.0.0",
    [int]    $Port        = 8080,

# HTTPS (optional)
    [switch] $UseHttps,
    [int]    $HttpsPort   = 8443,
    [string] $KeystorePath       = ".\keystore.jks",          # or .\dev-keystore.p12
    [ValidateSet('JKS','PKCS12')]
    [string] $KeystoreType       = "JKS",
    [string] $KeyAlias           = "local-dev",
    [string] $KeystorePassword   = "changeit",
    [string] $PrivateKeyPassword = "",                        # defaults to KeystorePassword if empty

# Gradle entrypoint
    [string] $GradleTask  = ":server:run",

# Environment (affects DB defaults)
    [ValidateSet('dev','prod')]
    [string] $Env         = 'dev',

# DB
    [string] $DbHost      = "127.0.0.1",
    [int]    $DbPort      = 5432,
    [string] $DbName      = "ApnaFund",
    [string] $DbUser,
    [string] $DbPassword,
    [string] $DbSchema,

# Pool tuning
    [int]    $DbPoolSize         = 10,
    [int]    $DbMinIdle          = 1,
    [int]    $DbConnectTimeoutMs = 30000,
    [int]    $DbIdleTimeoutMs    = 600000,
    [int]    $DbMaxLifetimeMs    = 1800000,

# Optional toggles
    [switch] $SkipFlyway,
    [switch] $SkipDb,

# JDK
    [string] $JdkHome = 'C:\Program Files\Android\Android Studio\jbr'
)

$ErrorActionPreference = "Stop"

# ----- Defaults by env -----
if (-not $DbUser)     { $DbUser     = if ($Env -eq 'prod') { 'apnafund_prod_user' } else { 'apnafund_dev_user' } }
if (-not $DbPassword) { $DbPassword = if ($Env -eq 'prod') { 'apna@produ3er'      } else { 'apna@devu3er'      } }
if (-not $DbSchema)   { $DbSchema   = if ($Env -eq 'prod') { 'ApnaFund'           } else { 'ApnaFundDev'       } }

# ----- Java -----
$env:JAVA_HOME = $JdkHome
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
& "$env:JAVA_HOME\bin\java.exe" -version | Write-Host

# ----- Must run from project root -----
if (-not (Test-Path -LiteralPath ".\gradlew")) {
    Write-Host "Please run this script from the project root (where gradlew is located)." -ForegroundColor Red
    exit 1
}

# ----- HTTPS keystore check (only when enabled) -----
if ($UseHttps) {
    if (-not (Test-Path -LiteralPath $KeystorePath)) {
        Write-Host "Keystore not found at '$KeystorePath'." -ForegroundColor Red
        Write-Host "Generate one, e.g. (PKCS12):" -ForegroundColor Yellow
        Write-Host "  keytool -genkeypair -alias $KeyAlias -keyalg RSA -storetype PKCS12 -keystore dev-keystore.p12 -storepass changeit -keypass changeit -dname `"CN=localhost`""
        exit 1
    }
}

# ----- Show config -----
Write-Host "▶ Starting server..." -ForegroundColor Cyan
Write-Host "   Env:         $Env"
Write-Host "   Host:        $ListenHost"
Write-Host "   HTTP Port:   $Port"
if ($UseHttps) {
    Write-Host "   HTTPS Port:  $HttpsPort"
    Write-Host "   Keystore:    $(Resolve-Path $KeystorePath)"
    Write-Host "   Type/Alias:  $KeystoreType / $KeyAlias"
} else {
    Write-Host "   HTTPS:       disabled"
}
Write-Host "   DB:          ${DbHost}:${DbPort}/${DbName}  (schema=${DbSchema})"
Write-Host "   DB_USER:     $DbUser"
Write-Host "   Pool:        size=$DbPoolSize minIdle=$DbMinIdle"
Write-Host "   Timeouts:    connect=$DbConnectTimeoutMs ms, idle=$DbIdleTimeoutMs ms, maxLifetime=$DbMaxLifetimeMs ms"
Write-Host "   SkipFlyway:  $($SkipFlyway.IsPresent)  SkipDb: $($SkipDb.IsPresent)"

# ----- Export env for Application.kt (HTTP/HTTPS) -----
$env:HOST      = "$ListenHost"
$env:HTTP_PORT = "$Port"

if ($UseHttps) {
    $env:USE_HTTPS = "1"
    $env:HTTPS_PORT = "$HttpsPort"
    $resolvedKs = (Resolve-Path $KeystorePath)
    $env:KEYSTORE_PATH = "$resolvedKs"
    $env:KEYSTORE_TYPE = "$KeystoreType"    # JKS or PKCS12
    $env:KEY_ALIAS     = "$KeyAlias"
    $env:KEYSTORE_PASSWORD = "$KeystorePassword"
    $env:PRIVATE_KEY_PASSWORD = "$(if ($PrivateKeyPassword) { $PrivateKeyPassword } else { $KeystorePassword })"
} else {
    Remove-Item Env:USE_HTTPS,Env:HTTPS_PORT,Env:KEYSTORE_PATH,Env:KEYSTORE_TYPE,Env:KEY_ALIAS,Env:KEYSTORE_PASSWORD,Env:PRIVATE_KEY_PASSWORD -ErrorAction SilentlyContinue
}

# ----- Export DB env vars (Db.kt) -----
$env:SERVER_PORT            = "$Port"
$env:DB_HOST                = "$DbHost"
$env:DB_PORT                = "$DbPort"
$env:DB_NAME                = "$DbName"
$env:DB_USER                = "$DbUser"
$env:DB_PASSWORD            = "$DbPassword"
$env:DB_SCHEMA              = "$DbSchema"

$env:DB_POOL_SIZE           = "$DbPoolSize"
$env:DB_MIN_IDLE            = "$DbMinIdle"
$env:DB_CONNECT_TIMEOUT_MS  = "$DbConnectTimeoutMs"
$env:DB_IDLE_TIMEOUT_MS     = "$DbIdleTimeoutMs"
$env:DB_MAX_LIFETIME_MS     = "$DbMaxLifetimeMs"

# Optional legacy/jdbc url for logs
$env:DB_URL = "jdbc:postgresql://${DbHost}:${DbPort}/${DbName}?currentSchema=${DbSchema}"

if ($SkipFlyway) { $env:SKIP_FLYWAY = "1" } else { Remove-Item Env:SKIP_FLYWAY -ErrorAction SilentlyContinue }
if ($SkipDb)     { $env:SKIP_DB     = "1" } else { Remove-Item Env:SKIP_DB     -ErrorAction SilentlyContinue }

# ----- Run -----
./gradlew $GradleTask --stacktrace --console=plain
