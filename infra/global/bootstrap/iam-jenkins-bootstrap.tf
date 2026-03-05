// infra/global/bootstrap/iam-jenkins-bootstrap.tf
# Creates jenkins-bot user and gives it permission to assume the deploy role.
# It references module.jenkins_deploy_role.role_arn directly (no root variable required).

resource "aws_iam_user" "jenkins_bot" {
  name = "jenkins-bot"
  tags = { CreatedBy = "terraform", Purpose = "bootstrap-assume-role" }
}

# Build an inline policy document that allows only sts:AssumeRole on the role created by the module.
data "aws_iam_policy_document" "jenkins_assume_policy" {
  statement {
    effect    = "Allow"
    actions   = ["sts:AssumeRole"]
    resources = [ module.jenkins_deploy_role.role_arn ]
  }
}

resource "aws_iam_user_policy" "jenkins_assume" {
  name   = "jenkins-bot-AssumeDeployRole"
  user   = aws_iam_user.jenkins_bot.name
  policy = data.aws_iam_policy_document.jenkins_assume_policy.json
}

# Optional: create an access key only if requested (keep false by default)
resource "aws_iam_access_key" "jenkins_bot_key" {
  count = var.create_jenkins_access_key ? 1 : 0
  user  = aws_iam_user.jenkins_bot.name
}

output "jenkins_bot_user_arn" {
  value = aws_iam_user.jenkins_bot.arn
}

output "jenkins_bot_access_key_id" {
  value       = try(aws_iam_access_key.jenkins_bot_key[0].id, "")
  description = "Access key id (if created)"
}
