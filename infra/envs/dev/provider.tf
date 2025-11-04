terraform {
  required_version = ">= 1.7.0"

  backend "s3" {}  # values come from backend.hcl at init time

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  profile = var.aws_profile
  region  = var.aws_region
}
