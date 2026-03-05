<#
bootstrap-jenkins-setup.ps1

Usage examples (from anywhere):
  # default: runs plan then apply
  .\bootstrap-jenkins-setup.ps1

  # only terraform init
  .\bootstrap-jenkins-setup.ps1 -Action init

  # only plan (uses -var-file)
  .\bootstrap-jenkins-setup.ps1 -Action plan

  # plan then apply (default behavior)
  .\bootstrap-jenkins-setup.ps1 -Action plan-and-apply

  # if TF creates an access key and you want the script to extract the secret:
  .\bootstrap-jenkins-setup.ps1 -KeepAccessKey -Action plan-and-apply

Parameters:
  -EnvDir        relative path from the script file to the bootstrap workspace (default: "..\global\bootstrap")
  -VarFile       tfvars filename inside the bootstrap workspace (default: "terraform.tfvars")
  -Action        one of: init, plan, apply, plan-and-apply (default: plan-and-apply)
  -KeepAccessKey switch: if present and TF created an aws_iam_access_key for jenkins-bot, extract the secret.
#>

param(
    [string]$EnvDir = "..\global\bootstrap",
    [string]$VarFile = "terraform.tfvars",
    [ValidateSet("init","plan","apply","plan-and-apply")]
    [string]$Action = "plan-and-apply",
    [switch]$KeepAccessKey = $false
)

Write-Host "=== ApnaFund Terraform Bootstrap Runner ===" -ForegroundColor Cyan
$ScriptDir = $PSScriptRoot
$OrigDir  = Get-Location

try {
    # Resolve bootstrap path
    $BootstrapPath = Resolve-Path -Path (Join-Path $ScriptDir $EnvDir) -ErrorAction Stop
    $BootstrapPath = $BootstrapPath.Path
    Write-Host "Bootstrap path   : $BootstrapPath" -ForegroundColor Yellow

    # Validate tfvars existence if required
    $VarFilePath = Join-Path $BootstrapPath $VarFile
    if ($Action -in @("plan","apply","plan-and-apply") -and -not (Test-Path $VarFilePath)) {
        throw "terraform var-file not found at: $VarFilePath"
    }

    # Helper to run command and stop on failure
    function Run-Checked([string[]]$cmdAndArgs) {
        Write-Host ">>> $($cmdAndArgs -join ' ')" -ForegroundColor Yellow
        & $cmdAndArgs[0] @($cmdAndArgs[1..($cmdAndArgs.Length-1)])
        if ($LASTEXITCODE -ne 0) {
            throw "Command failed (exit $LASTEXITCODE): $($cmdAndArgs -join ' ')"
        }
    }

    # Move into bootstrap workspace
    Set-Location -Path $BootstrapPath

    # INIT if requested or always before plan/apply
    if ($Action -in @("init","plan","apply","plan-and-apply")) {
        Run-Checked @("terraform", "init")
    }

    if ($Action -eq "plan") {
        Run-Checked @("terraform","plan","-var-file=$VarFilePath")
        Write-Host "`nCompleted: terraform plan" -ForegroundColor Green
    }
    elseif ($Action -eq "apply") {
        Run-Checked @("terraform","plan","-var-file=$VarFilePath","-out=tfplan")
        Run-Checked @("terraform","apply","-auto-approve","tfplan")
        Write-Host "`nCompleted: terraform apply" -ForegroundColor Green
    }
    elseif ($Action -eq "plan-and-apply") {
        Run-Checked @("terraform","plan","-var-file=$VarFilePath","-out=tfplan")
        Run-Checked @("terraform","apply","-auto-approve","tfplan")
        Write-Host "`nCompleted: terraform plan + apply" -ForegroundColor Green
    }
    elseif ($Action -eq "init") {
        # init already run above
        Write-Host "`nCompleted: terraform init" -ForegroundColor Green
    }

    # Optionally extract access key secret if requested
    if ($KeepAccessKey) {
        $stateList = & terraform state list 2>$null
        if ($LASTEXITCODE -ne 0) {
            throw "Unable to list terraform state. Did apply succeed?"
        }

        $akAddress = ($stateList -split "`n" | Where-Object { $_ -match "^aws_iam_access_key\.jenkins_bot_key" } | Select-Object -First 1)
        if (-not $akAddress) {
            Write-Host "No aws_iam_access_key.jenkins_bot_key resource found in state. create_jenkins_access_key likely false." -ForegroundColor Yellow
        } else {
            $akAddress = $akAddress.Trim()
            Write-Host "Found access key resource: $akAddress" -ForegroundColor Cyan

            $stateShow = & terraform state show -no-color $akAddress 2>$null
            if ($LASTEXITCODE -ne 0 -or -not $stateShow) {
                throw "Failed to read state for $akAddress"
            }

            $secretLine = ($stateShow -split "`n" | Where-Object { $_ -match '^\s*secret\s*=' } | Select-Object -First 1)
            if (-not $secretLine) {
                Write-Host "Secret attribute not present in state output. The key may not be new or accessible." -ForegroundColor Red
            } else {
                # Extract secret (strip leading 'secret = ' and surrounding quotes)
                $secret = $secretLine -replace '^\s*secret\s*=\s*"', '' -replace '"\s*$', ''
                $outFile = Join-Path $BootstrapPath "jenkins-bot-access-key.secret"
                Set-Content -Path $outFile -Value $secret -Encoding ASCII

                # Tighten permissions (best-effort)
                try {
                    icacls $outFile /inheritance:r | Out-Null
                    $user = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
                    icacls $outFile /grant:r "$($user):(R,W)" | Out-Null
                } catch {
                    Write-Host "Warning: could not adjust file ACLs. Please secure $outFile manually." -ForegroundColor Yellow
                }

                Write-Host "`n*** IMPORTANT ***" -ForegroundColor Red
                Write-Host "The jenkins-bot access key secret has been written to: $outFile" -ForegroundColor Yellow
                Write-Host "Copy the secret into Jenkins Credentials immediately (username = access key id, password = secret)." -ForegroundColor Yellow
                Write-Host "After storing in Jenkins: set create_jenkins_access_key = false in terraform.tfvars and re-run apply to remove key from TF state." -ForegroundColor Yellow
            }
        }
    }

    # Show terraform outputs
    Write-Host "`nTerraform outputs:" -ForegroundColor Cyan
    & terraform output

} catch {
    Write-Host "`nERROR: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "See above output for details." -ForegroundColor Red
} finally {
    # Always return to the original directory the user started in
    Set-Location -Path $OrigDir
    Write-Host "`nReturned to: $OrigDir" -ForegroundColor DarkGray
}

Write-Host "`nDone." -ForegroundColor Green
