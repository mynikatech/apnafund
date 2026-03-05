// infra/modules/iam/jenkins-deploy-role/outputs.tf
output "role_name" {
  value = aws_iam_role.this.name
}

output "role_arn" {
  value = aws_iam_role.this.arn
}