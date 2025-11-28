############################################
# Terraform & Providers are in provider.tf
############################################

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
data "aws_vpc" "default" { default = true }

data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_subnet" "default" {
  vpc_id            = data.aws_vpc.default.id
  availability_zone = data.aws_availability_zones.available.names[0]
  default_for_az    = true
}

############################################
# S3: Config bucket (holds startup scripts)
############################################
resource "aws_s3_bucket" "config" {
  bucket = var.config_bucket
  tags   = { Env = "dev", App = "ApnaFund" }
}

# Strongly recommended: block public access
resource "aws_s3_bucket_public_access_block" "config" {
  bucket                  = aws_s3_bucket.config.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# (Optional) versioning for safe rollbacks
resource "aws_s3_bucket_versioning" "config" {
  bucket = aws_s3_bucket.config.id
  versioning_configuration {
    status = "Enabled"
  }
}

# Ownership controls + private ACL (new S3 requirements)
resource "aws_s3_bucket_ownership_controls" "config" {
  bucket = aws_s3_bucket.config.id
  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

resource "aws_s3_bucket_acl" "config" {
  depends_on = [aws_s3_bucket_ownership_controls.config]
  bucket     = aws_s3_bucket.config.id
  acl        = "private"
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

resource "local_file" "dev_key_private" {
  filename        = "${path.module}/apnafund-dev-key.pem"
  content         = tls_private_key.dev_key.private_key_pem
  file_permission = "0600"
}

############################################
# Security Group
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

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "ApnaFund Dev Server SG", Env = "dev" }
}

############################################
# Instance Profile
############################################
resource "aws_iam_instance_profile" "deploy_profile" {
  name = "deployRoleProfile"
  role = module.iam_deploy_role.role_name
}

############################################
# AMI lookup: Amazon Linux 2023 (x86_64)
############################################
data "aws_ami" "amazon_linux" {
  # PIN (avoids replacement on AMI bumps). Use your known-good AMI below:
  owners      = ["amazon"]
  most_recent = false
  filter {
    name = "image-id"
    values = [var.ami_id]
  }
}

############################################
# Allow reading startup.sh from the config bucket
############################################
data "aws_iam_policy_document" "config_bucket_read" {
  statement {
    sid     = "ReadEnvStartup"
    effect  = "Allow"
    actions = ["s3:GetObject","s3:GetObjectVersion","s3:ListBucket"]
    resources = [
      "arn:aws:s3:::${var.config_bucket}",
      "arn:aws:s3:::${var.config_bucket}/apnafund/*"
    ]
  }
}

resource "aws_iam_policy" "config_bucket_read" {
  name   = "ApnaFundConfigBucketRead"
  policy = data.aws_iam_policy_document.config_bucket_read.json
}

resource "aws_iam_role_policy_attachment" "attach_config_bucket_read" {
  role       = module.iam_deploy_role.role_name
  policy_arn = aws_iam_policy.config_bucket_read.arn
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
  user_data              = file("${path.module}/user_data.sh")

  # Make these available to the script via EC2 env (IMDS not for env, so we use shell vars block)
  metadata_options {
    http_endpoint = "enabled"
    http_tokens   = "required"
  }

  # Root volume
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
# Optional: dedicated EBS for Postgres data
############################################
resource "aws_ebs_volume" "pg_data" {
  availability_zone = aws_instance.dev_server.availability_zone
  size              = 20
  type              = "gp3"
  encrypted         = true
  tags = { Name = "apnafund-dev-pg-data", Env = "dev" }
}

resource "aws_volume_attachment" "pg_data_attach" {
  device_name = "/dev/xvdf"
  instance_id = aws_instance.dev_server.id
  volume_id   = aws_ebs_volume.pg_data.id
}

############################################
# Attach SSM core (Session Manager)
############################################
resource "aws_iam_role_policy_attachment" "attach_ssm_core" {
  role       = module.iam_deploy_role.role_name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# Give user-data access to its config (via instance tags → NOT used in script; bucket/key are hard-coded above)
# You can also pass as TF vars and substitute into user_data.sh via sed/template if you prefer.

############################################
# Outputs
############################################
output "deploy_role_arn"         {
  value = module.iam_deploy_role.role_arn
}
output "dev_instance_public_ip"  {
  value = aws_instance.dev_server.public_ip
}
output "dev_instance_public_dns" {
  value = aws_instance.dev_server.public_dns
}

