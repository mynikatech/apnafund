// infra/modules/iam/jenkins-deploy-role/variables.tf
variable "role_name" {
  type        = string
  description = "Name for the Jenkins deploy IAM role"
}

variable "trusted_principals" {
  type        = list(string)
  description = "List of principal ARNs (AWS) allowed to assume this role (e.g. jenkins-bot user ARN)"
  default     = []
}

variable "managed_policy_arns" {
  type        = list(string)
  description = "Optional list of managed policy ARNs to attach to the role"
  default     = []
}

variable "inline_policy" {
  type        = string
  description = "Optional inline JSON policy (string)"
  default     = ""
}