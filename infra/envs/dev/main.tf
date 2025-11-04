############################################
# IAM Deploy Role (module)
############################################
module "iam_deploy_role" {
  source       = "../../modules/iam/deploy-role"
  role_name    = var.deploy_role_name
  attach_admin = true
}

############################################
# Networking: default VPC + a default subnet
############################################
data "aws_vpc" "default" {
  default = true
}

data "aws_availability_zones" "available" {
  state = "available"
}
# Pick any default subnet in one AZ (good enough for dev)
data "aws_subnet" "default" {
  vpc_id            = data.aws_vpc.default.id
  availability_zone = data.aws_availability_zones.available.names[0]
  default_for_az    = true
}

############################################
# EC2 Key Pair (Terraform-managed)
############################################
resource "tls_private_key" "dev_key" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "aws_key_pair" "dev_keypair" {
  key_name   = "apnafund-dev-key"
  public_key = tls_private_key.dev_key.public_key_openssh
}

# Save private key locally for SSH
resource "local_file" "dev_key_private" {
  filename = "${path.module}/apnafund-dev-key.pem"
  content  = tls_private_key.dev_key.private_key_pem
  file_permission = "0600"
}

############################################
# Security Group
# - SSH (22) only from your IP
# - HTTP (80) open
# - HTTPS (443) open
# - 8443 only from your IP (optional app/admin)
############################################
resource "aws_security_group" "dev_server_sg" {
  name        = "apnafund-dev-sg"
  description = "ApnaFund dev EC2 security group"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "SSH from admin IP"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.admin_ip]
  }

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "App port (8443) from admin IP"
    from_port   = 8443
    to_port     = 8443
    protocol    = "tcp"
    cidr_blocks = [var.admin_ip]
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "ApnaFund Dev Server SG"
    Env  = "dev"
  }
}

############################################
# Instance Profile (EC2 assumes deployRole)
############################################
resource "aws_iam_instance_profile" "deploy_profile" {
  name = "deployRoleProfile"
  role = module.iam_deploy_role.role_name
}

############################################
# AMI lookup: Amazon Linux 2023 (x86_64)
############################################
data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }
}

############################################
# EC2 Instance
############################################
resource "aws_instance" "dev_server" {
  ami                    = data.aws_ami.amazon_linux.id
  instance_type          = "t3.micro"
  subnet_id              = data.aws_subnet.default.id
  vpc_security_group_ids = [aws_security_group.dev_server_sg.id]
  iam_instance_profile   = aws_iam_instance_profile.deploy_profile.name
  key_name               = aws_key_pair.dev_keypair.key_name

  root_block_device {
    volume_type = "gp3"
    volume_size = 30
  }

  tags = {
    Name = "ApnaFund Dev Server"
    Env  = "dev"
  }
}

############################################
# OPTIONAL: Elastic IP (commented for dev)
# Uncomment these blocks when you want a stable IP.
############################################
# resource "aws_eip" "dev_eip" {
#   vpc = true
#   tags = {
#     Name = "ApnaFund Dev EIP"
#     Env  = "dev"
#   }
# }
#
# resource "aws_eip_association" "dev_eip_assoc" {
#   instance_id   = aws_instance.dev_server.id
#   allocation_id = aws_eip.dev_eip.id
# }

############################################
# Outputs
############################################
output "deploy_role_arn" {
  description = "ARN of the deploy role"
  value       = module.iam_deploy_role.role_arn
}

output "dev_instance_public_ip" {
  description = "Public IP of the dev EC2 instance"
  value       = aws_instance.dev_server.public_ip
}

output "dev_instance_public_dns" {
  description = "Public DNS of the dev EC2 instance"
  value       = aws_instance.dev_server.public_dns
}

output "dev_security_group_id" {
  description = "Security group ID for the dev instance"
  value       = aws_security_group.dev_server_sg.id
}