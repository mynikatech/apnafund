resource "aws_iam_role" "this" {
  name = var.role_name

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = { AWS = var.trusted_principals }
      Action = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy" "inline" {
  name   = "${var.role_name}-policy"
  role   = aws_iam_role.this.id
  policy = var.inline_policy
}