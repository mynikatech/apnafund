variable "aws_profile" {
  type    = string
  default = "ApnaFundAdmin"
}

variable "aws_region" {
  type    = string
  default = "ap-south-1"
}

# IAM deploy role name (from your module)
variable "deploy_role_name" {
  type    = string
  default = "deployRole"
}

# Your public IP with /32 mask. Replace this!
variable "admin_ip" {
  description = "Your public IP/CIDR for SSH and 8443 (e.g. 49.x.x.x/32)"
  type        = string
  default     = "49.36.106.43/32"
}

# Existing EC2 key pair name in ap-south-1
variable "key_pair_name" {
  description = "EC2 key pair name for SSH"
  type        = string
  default     = "apnafund-dev-key"
}