// infra/global/bootstrap/iam-attach.tf
# Attach the deploy policy (created in iam-deploy-policy.tf) to the role created by the module.

resource "aws_iam_role_policy_attachment" "attach_apnafund_deploy" {
  role       = module.jenkins_deploy_role.role_name
  policy_arn = aws_iam_policy.apnafund_deploy_policy.arn
}