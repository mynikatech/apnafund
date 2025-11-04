# ------------------------------
# Outputs
# ------------------------------

output "role_arn" {
  description = "ARN of the IAM role created for EC2 deployments"
  value       = aws_iam_role.this.arn
}

output "role_name" {
  description = "Name of the IAM role"
  value       = aws_iam_role.this.name
}
