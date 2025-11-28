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
  description = "public IP/CIDR for SSH and 8443 (e.g. 49.x.x.x/32)"
  type        = string
  default     = "49.36.106.43/32"
}

# Existing EC2 key pair name in ap-south-1
variable "key_pair_name" {
  description = "EC2 key pair name for SSH"
  type        = string
  default     = "apnafund-dev-key"
}

variable "postgres_admin_password"  {
  type = string
  sensitive = true
}
variable "postgres_deploy_password" {
  type = string
  sensitive = true
}
variable "postgres_app_password"    {
  type = string
  sensitive = true
}

variable "ami_id" {
  description = "AMI to use for the EC2 instance"
  type        = string
  default     = "ami-0eef31216cac38c98" # set to the one you want
}

variable "pg_volume_size_gb" {
  type        = number
  default     = 20
  description = "Size of the dedicated EBS volume for PostgreSQL data"
}

variable "pg_volume_type" {
  type        = string
  default     = "gp3"
}

variable "pg_device_name" {
  type        = string
  default     = "/dev/xvdf" # Linux will map to /dev/nvme1n1 on AL2023
}

variable "pg_delete_on_termination" {
  type        = bool
  default     = false  # keep data if instance is terminated
}

variable "config_bucket" {
  description = "S3 bucket that stores env startup scripts (e.g., apnafund-config-<acct>-ap-south-1)"
  type        = string
}