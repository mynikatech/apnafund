# ------------------------------
# IAM Role for EC2 Deployments
# ------------------------------

resource "aws_iam_role" "this" {
  name = var.role_name

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect    = "Allow",
        Principal = {
          Service = "ec2.amazonaws.com"
        },
        Action = "sts:AssumeRole"
      }
    ]
  })
}

# Optional AdministratorAccess attachment
resource "aws_iam_role_policy_attachment" "admin" {
  count = var.attach_admin ? 1 : 0

  role       = aws_iam_role.this.name
  policy_arn = "arn:aws:iam::aws:policy/AdministratorAccess"
}
