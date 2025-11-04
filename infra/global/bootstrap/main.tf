# ------------------------------
# S3 bucket for Terraform state
# ------------------------------
resource "aws_s3_bucket" "state" {
  bucket = var.state_bucket_name

  lifecycle {
    prevent_destroy = true
  }

  tags = {
    Name  = "Terraform Backend Bucket"
    Scope = "Global"
  }
}

resource "aws_s3_bucket_versioning" "state" {
  bucket = aws_s3_bucket.state.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "state" {
  bucket = aws_s3_bucket.state.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "state" {
  bucket                  = aws_s3_bucket.state.id
  block_public_acls       = true
  ignore_public_acls      = true
  block_public_policy     = true
  restrict_public_buckets = true
}

# ------------------------------
# DynamoDB table for state lock
# ------------------------------
resource "aws_dynamodb_table" "locks" {
  name         = var.state_lock_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "LockID"

  attribute {
    name = "LockID"
    type = "S"
  }

  tags = {
    Name  = "Terraform Lock Table"
    Scope = "Global"
  }
}

# IAM policy for backend access
resource "aws_iam_policy" "terraform_backend_access" {
  name        = "TerraformBackendAccessPolicy"
  description = "Allow Terraform to manage its backend (S3 + DynamoDB)"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["s3:ListBucket", "s3:GetBucketLocation"]
        Resource = "arn:aws:s3:::${aws_s3_bucket.state.bucket}"
      },
      {
        Effect   = "Allow"
        Action   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
        Resource = "arn:aws:s3:::${aws_s3_bucket.state.bucket}/*"
      },
      {
        Effect   = "Allow"
        Action   = [
          "dynamodb:DescribeTable",
          "dynamodb:GetItem",
          "dynamodb:PutItem",
          "dynamodb:DeleteItem"
        ]
        Resource = aws_dynamodb_table.locks.arn
      }
    ]
  })
}

# Attach to ApnaFundAdmin user
resource "aws_iam_user_policy_attachment" "attach_backend_policy" {
  user       = "ApnaFundAdmin"
  policy_arn = aws_iam_policy.terraform_backend_access.arn
}


# ------------------------------
# Outputs
# ------------------------------
output "state_bucket_name" {
  value = aws_s3_bucket.state.bucket
}

output "state_lock_table_name" {
  value = aws_dynamodb_table.locks.name
}
