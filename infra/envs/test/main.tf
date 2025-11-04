module "iam_deploy_role" {
  source        = "../../modules/iam/deploy-role"
  role_name     = var.deploy_role_name
  attach_admin  = true
}

output "deploy_role_arn" {
  value       = module.iam_deploy_role.role_arn
  description = "ARN of the deploy IAM role"
}