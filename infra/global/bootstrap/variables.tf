variable "aws_profile" {
  type    = string
  default = "ApnaFundAdmin"
}

variable "aws_region" {
  type    = string
  default = "ap-south-1"
}

variable "state_bucket_name" {
  type    = string
  default = "apnafund-terraform-backend"
}

variable "state_lock_table_name" {
  type    = string
  default = "terraform-locks"
}

variable "backend_access_principal_type" {
  description = "Principal type to attach backend access policy to: user or role"
  type        = string
  default     = "role" # set "user" if you really need user-attachment
  validation {
    condition     = contains(["user", "role"], var.backend_access_principal_type)
    error_message = "backend_access_principal_type must be 'user' or 'role'."
  }
}

variable "backend_access_principal_name" {
  description = "User or Role name to attach the backend access policy to"
  type        = string
  default     = "YOUR_ROLE_NAME_HERE" # e.g., TerraformRunnerRole or whatever STS shows after assumed-role/
}