variable "bucket_name" {
  type        = string
  description = "S3 bucket name for static public assets"
}

variable "env" {
  type        = string
  description = "Environment name (dev/prod)"
}

resource "aws_s3_bucket" "assets" {
  bucket = var.bucket_name

  tags = {
    App = "ApnaFund"
    Env = var.env
    Type = "static-assets"
  }
}

# Required for modern S3
resource "aws_s3_bucket_ownership_controls" "assets" {
  bucket = aws_s3_bucket.assets.id

  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

# Allow public reads (images, css, etc.)
resource "aws_s3_bucket_public_access_block" "assets" {
  bucket = aws_s3_bucket.assets.id

  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

resource "aws_s3_bucket_acl" "assets" {
  depends_on = [
    aws_s3_bucket_ownership_controls.assets,
    aws_s3_bucket_public_access_block.assets
  ]

  bucket = aws_s3_bucket.assets.id
  acl    = "public-read"
}

# Public read policy
resource "aws_s3_bucket_policy" "assets" {
  bucket = aws_s3_bucket.assets.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "PublicReadGetObject"
        Effect    = "Allow"
        Principal = "*"
        Action    = "s3:GetObject"
        Resource  = "${aws_s3_bucket.assets.arn}/*"
      }
    ]
  })
}

output "bucket_name" {
  value = aws_s3_bucket.assets.bucket
}

output "bucket_arn" {
  value = aws_s3_bucket.assets.arn
}

output "public_base_url" {
  value = "https://${aws_s3_bucket.assets.bucket}.s3.${data.aws_region.current.name}.amazonaws.com"
}

data "aws_region" "current" {}
