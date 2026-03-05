resource "aws_iam_role" "this" {
  name = "${var.app_name}-${var.env}-app-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Service = "ec2.amazonaws.com"
      }
      Action = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_instance_profile" "this" {
  name = "${var.app_name}-${var.env}-app-profile"
  role = aws_iam_role.this.name
}