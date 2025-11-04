variable "role_name" {
  type        = string
  description = "Name of the IAM role to create"
}

variable "attach_admin" {
  type        = bool
  default     = true
  description = "Attach the AdministratorAccess policy to this role"
}
