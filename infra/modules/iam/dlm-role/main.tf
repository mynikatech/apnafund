resource "aws_iam_role" "this" {
  name = "${var.app_name}-${var.env}-dlm-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Service = "dlm.amazonaws.com"
      }
      Action = "sts:AssumeRole"
    }]
  })

  tags = {
    App = var.app_name
    Env = var.env
  }
}
