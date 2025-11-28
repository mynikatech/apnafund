bucket         = "apnafund-terraform-backend"
key            = "dev/terraform.tfstate"
region         = "ap-south-1"
dynamodb_table = "terraform-locks"
encrypt        = true
profile        = "jenkins-bot"