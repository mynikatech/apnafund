Write-Host "=== Bootstrapping Terraform Backend Infrastructure ==="

Set-Location "$PSScriptRoot"

# Initialize Terraform without a backend (local)
terraform init -backend=false

# Apply to create S3 + DynamoDB
terraform apply -auto-approve

Write-Host "=== Bootstrap complete. S3 bucket and DynamoDB table created. ==="