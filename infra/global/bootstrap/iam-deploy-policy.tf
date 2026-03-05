// infra/global/bootstrap/iam-deploy-policy.tf
data "aws_caller_identity" "current" {}

variable "config_bucket" {
  type = string
  description = "Config bucket name (used by deploy policy)"
}

variable "region" {
  type = string
  description = "AWS region"
}

resource "aws_iam_policy" "apnafund_deploy_policy" {
  name        = "ApnaFund-DeployPolicy"
  description = "Permissions for CI/CD to deploy ApnaFund infra and app (restrict this!)."

  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Sid = "S3Access",
        Effect = "Allow",
        Action = ["s3:GetObject","s3:PutObject","s3:ListBucket"],
        Resource = [
          "arn:aws:s3:::${var.config_bucket}",
          "arn:aws:s3:::${var.config_bucket}/*"
        ]
      },
      {
        Sid = "CloudFormation",
        Effect = "Allow",
        Action = [
          "cloudformation:CreateStack",
          "cloudformation:UpdateStack",
          "cloudformation:DescribeStacks",
          "cloudformation:DeleteStack",
          "cloudformation:ListStacks",
          "cloudformation:GetTemplate"
        ],
        Resource = "arn:aws:cloudformation:${var.region}:${data.aws_caller_identity.current.account_id}:stack/*"
      },
      {
        Sid = "PassRole",
        Effect = "Allow",
        Action = ["iam:PassRole"],
        Resource = ["arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/*"]
      },
      {
        Sid = "ECR",
        Effect = "Allow",
        Action = [
          "ecr:GetAuthorizationToken",
          "ecr:BatchGetImage",
          "ecr:PutImage",
          "ecr:InitiateLayerUpload",
          "ecr:UploadLayerPart",
          "ecr:CompleteLayerUpload"
        ],
        Resource = "*"
      },
      {
        Sid = "SSMParameter",
        Effect = "Allow",
        Action = ["ssm:GetParameter","ssm:GetParameters","ssm:GetParameterHistory"],
        Resource = "arn:aws:ssm:${var.region}:${data.aws_caller_identity.current.account_id}:parameter/*"
      }
    ]
  })
}
