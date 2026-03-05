############################################
# Terraform & Providers are in provider.tf
############################################

############################################
# IAM Deploy Role (module)
############################################
module "iam_deploy_role" {
  source            = "../../modules/iam/deploy-role"
  role_name         = var.deploy_role_name
  inline_policy     = data.aws_iam_policy_document.ec2_policy.json
}

module "jenkins_role" {
  source            = "../../modules/iam/jenkins-deploy-role"
  role_name         = "ApnaFundJenkinsDeployRole"
  trusted_principals = ["arn:aws:iam::861082243595:user/jenkins-bot"]
  inline_policy     = data.aws_iam_policy_document.jenkins_combined.json
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
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
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

  lifecycle {
    prevent_destroy = true
  }
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

data "aws_iam_policy_document" "ec2_policy" {
  # SSM agent core permissions
  statement {
    effect = "Allow"
    actions = [
      "ssm:DescribeAssociation",
      "ssm:GetDeployablePatchSnapshotForInstance",
      "ssm:GetDocument",
      "ssm:DescribeDocument",
      "ssm:GetManifest",
      "ssm:GetParameter",
      "ssm:GetParameters",
      "ssm:ListAssociations",
      "ssm:ListInstanceAssociations",
      "ssm:PutInventory",
      "ssm:PutComplianceItems",
      "ssm:PutConfigurePackageResult",
      "ssm:UpdateAssociationStatus",
      "ssm:UpdateInstanceAssociationStatus",
      "ssm:UpdateInstanceInformation"
    ]
    resources = ["*"]
  }

  # SSM Messages
  statement {
    effect = "Allow"
    actions = [
      "ssmmessages:CreateControlChannel",
      "ssmmessages:CreateDataChannel",
      "ssmmessages:OpenControlChannel",
      "ssmmessages:OpenDataChannel"
    ]
    resources = ["*"]
  }

  # EC2 messages
  statement {
    effect = "Allow"
    actions = [
      "ec2messages:AcknowledgeMessage",
      "ec2messages:DeleteMessage",
      "ec2messages:FailMessage",
      "ec2messages:GetEndpoint",
      "ec2messages:GetMessages",
      "ec2messages:SendReply"
    ]
    resources = ["*"]
  }

  # Allow reading bootstrap files from S3
  statement {
    effect = "Allow"
    actions = ["s3:GetObject"]
    resources = ["arn:aws:s3:::apnafund-config/*"]
  }
}


data "aws_iam_policy_document" "jenkins_combined" {

  # === Existing S3 access ===
  statement {
    sid     = "S3Access"
    effect  = "Allow"
    actions = [
      "s3:GetObject",
      "s3:PutObject",
      "s3:ListBucket"
    ]
    resources = [
      "arn:aws:s3:::apnafund-config",
      "arn:aws:s3:::apnafund-config/*"
    ]
  }

  # === CloudFormation permissions ===
  statement {
    sid = "CloudFormation"
    effect = "Allow"
    actions = [
      "cloudformation:CreateStack",
      "cloudformation:UpdateStack",
      "cloudformation:DescribeStacks",
      "cloudformation:DeleteStack",
      "cloudformation:ListStacks",
      "cloudformation:GetTemplate"
    ]
    resources =  [
      "arn:aws:cloudformation:ap-south-1:861082243595:stack/*"
    ]
  }

  # === PassRole ===
  statement {
    sid    = "PassRole"
    effect = "Allow"
    actions = [
      "iam:PassRole"
    ]
    resources = [
      "arn:aws:iam::861082243595:role/*"
    ]
  }

  # === ECR ===
  statement {
    sid = "ECR"
    effect = "Allow"
    actions = [
      "ecr:GetAuthorizationToken",
      "ecr:BatchGetImage",
      "ecr:PutImage",
      "ecr:InitiateLayerUpload",
      "ecr:UploadLayerPart",
      "ecr:CompleteLayerUpload"
    ]
    resources = ["*"]
  }

  # === SSM Parameter Store ===
  statement {
    sid    = "SSMParameter"
    effect = "Allow"
    actions = [
      "ssm:GetParameter",
      "ssm:GetParameters",
      "ssm:GetParameterHistory"
    ]
    resources = [
      "arn:aws:ssm:ap-south-1:861082243595:parameter/*"
    ]
  }

  # === EC2 Describe (REQUIRED FOR SSM SCRIPT) ===
  statement {
    sid = "EC2Describe"
    effect = "Allow"
    actions = [
      "ec2:DescribeInstances",
      "ec2:DescribeInstanceStatus",
      "ec2:DescribeInstanceAttribute",
      "ec2:DescribeTags"
    ]
    resources = ["*"]
  }

  # === SSM RunCommand (REQUIRED FOR ssm_refresh_dev.ps1) ===
  statement {
    sid = "SSMRunCommand"
    effect = "Allow"
    actions = [
      "ssm:SendCommand",
      "ssm:GetCommandInvocation",
      "ssm:DescribeInstanceInformation"
    ]
    resources = ["*"]
  }
}

###########################################
# EIP creation and association
###########################################
resource "aws_eip" "dev_eip" {
  tags = {
    Name = "apnafund-dev-eip"
    Env  = "dev"
  }
}

resource "aws_eip_association" "dev_eip_assoc" {
  instance_id   = aws_instance.dev_server.id
  allocation_id = aws_eip.dev_eip.id
}


# --- s3 app bucket ---
resource "aws_s3_bucket" "app" {
  bucket = "apnafund-app-861082243595-ap-south-1"
  tags   = { Env = "dev", App = "ApnaFund" }
}

resource "aws_s3_bucket_public_access_block" "app" {
  bucket                  = aws_s3_bucket.app.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_versioning" "app" {
  bucket = aws_s3_bucket.app.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_ownership_controls" "app" {
  bucket = aws_s3_bucket.app.id
  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

resource "aws_s3_bucket_acl" "app" {
  depends_on = [aws_s3_bucket_ownership_controls.app]
  bucket     = aws_s3_bucket.app.id
  acl        = "private"
}

# --- IAM policy to allow module.iam_deploy_role to read app objects ---
data "aws_iam_policy_document" "app_bucket_read" {
  statement {
    sid     = "AllowListAndGetForAppBucket"
    effect  = "Allow"
    actions = [
      "s3:ListBucket"
    ]
    resources = [
      aws_s3_bucket.app.arn
    ]
    condition {
      test     = "StringLike"
      variable = "s3:prefix"
      values   = ["apnafund/dev/*", "apnafund/prod/*", "apnafund/releases/*"]
    }
  }

  statement {
    sid     = "AllowGetObjects"
    effect  = "Allow"
    actions = [
      "s3:GetObject"
    ]
    resources = [
      "${aws_s3_bucket.app.arn}/*"
    ]
  }
}

resource "aws_iam_policy" "app_bucket_read" {
  name   = "ApnaFundAppBucketRead"
  policy = data.aws_iam_policy_document.app_bucket_read.json
}

resource "aws_iam_role_policy_attachment" "attach_app_bucket_read" {
  role       = module.iam_deploy_role.role_name
  policy_arn = aws_iam_policy.app_bucket_read.arn
}

# --- Jenkins write policy: allow Jenkins to publish artifacts to app bucket ---
data "aws_iam_policy_document" "app_write_jenkins" {
  statement {
    sid     = "AllowListBucketForJenkins"
    effect  = "Allow"
    actions = ["s3:ListBucket"]
    resources = [ aws_s3_bucket.app.arn ]
    condition {
      test     = "StringLike"
      variable = "s3:prefix"
      values   = ["dev/*", "prod/*", "releases/*"]
    }
  }

  statement {
    sid     = "AllowPutGetDeleteObjectsForJenkins"
    effect  = "Allow"
    actions = ["s3:PutObject","s3:GetObject","s3:DeleteObject"]
    resources = [
      "${aws_s3_bucket.app.arn}/dev/*",
      "${aws_s3_bucket.app.arn}/prod/*",
      "${aws_s3_bucket.app.arn}/releases/*"
    ]
  }
}

resource "aws_iam_policy" "app_write_policy" {
  name   = "ApnaFundAppWriteForJenkins"
  policy = data.aws_iam_policy_document.app_write_jenkins.json
}

resource "aws_iam_role_policy_attachment" "attach_app_write_to_jenkins" {
  role       = module.jenkins_role.role_name
  policy_arn = aws_iam_policy.app_write_policy.arn
}

############################################
# Route53 record: api-dev.apnafund.mynikatech.in → EC2 EIP
############################################

data "aws_route53_zone" "mynikatech" {
  name         = "mynikatech.in."
  private_zone = false
}
resource "aws_route53_record" "api_dev" {
  zone_id = data.aws_route53_zone.mynikatech.zone_id
  name    = "api-dev.apnafund.mynikatech.in"
  type    = "A"
  ttl     = 60
  records = [aws_eip.dev_eip.public_ip]
}

############################################
# IAM: Allow deployRole to update Route53 records (Certbot DNS-01)
############################################
data "aws_iam_policy_document" "route53_change_for_deployrole" {
  statement {
    sid     = "ListZonesAndGetChange"
    effect  = "Allow"
    actions = [
      "route53:ListHostedZones",
      "route53:GetChange"
    ]
    resources = ["*"]
  }

  statement {
    sid     = "ChangeRecordSetsForHostedZone"
    effect  = "Allow"
    actions = [
      "route53:ChangeResourceRecordSets"
    ]
    resources = [
      "arn:aws:route53:::hostedzone/${data.aws_route53_zone.mynikatech.zone_id}"
    ]
  }
}

resource "aws_iam_policy" "deploy_role_route53" {
  name   = "ApnaFundDeployRoleRoute53Change"
  policy = data.aws_iam_policy_document.route53_change_for_deployrole.json
}

resource "aws_iam_role_policy_attachment" "attach_route53_to_deployrole" {
  role       = module.iam_deploy_role.role_name
  policy_arn = aws_iam_policy.deploy_role_route53.arn
}

# New app role to use the SNS and SQS topis and topics as well

module "notifications" {
  source   = "../../modules/notifications"
  env      = "dev"
  app_name = "apnafund"
}

module "apnafund_app_role" {
  source         = "../../modules/iam/apnafund-app-role"
  env            = "dev"
  app_name       = "apnafund"
  sns_topic_arns = module.notifications.sns_topic_arns
}

# Lambda Role
module "email_lambda_role" {
  source = "../../modules/iam/lambda-email-role"

  env = "dev"
  ses_from_email = "support@mynikatech.in"
}

resource "aws_lambda_function" "email_processor" {
  function_name = "apnafund-email-processor"
  role   = module.email_lambda_role.role_arn
  runtime = "java21"
  handler = "com.mynikatech.apnafund.lambda.email.EmailProcessorHandler::handleRequest"
  filename         = "${path.root}/../../artifacts/email-processor.jar"
  source_code_hash = filebase64sha256("${path.root}/../../artifacts/email-processor.jar")
  timeout     = 30
  memory_size = 256

  environment {
    variables = {
      SES_FROM_EMAIL = "support@mynikatech.in"
    }
  }
}
resource "aws_lambda_event_source_mapping" "email_processor_sqs" {
  event_source_arn = module.notifications.email_queue_arn
  function_name    = aws_lambda_function.email_processor.arn

  batch_size                         = 10
  maximum_batching_window_in_seconds = 10
  enabled                            = true
}

module "whatsapp_lambda_role" {
  source = "../../modules/iam/lambda-whatsapp-role"

  env = "dev"
}

resource "aws_lambda_function" "whatsapp_processor" {
  function_name = "apnafund-whatsapp-processor"

  role   = module.whatsapp_lambda_role.role_arn
  runtime = "java21"
  handler = "com.mynikatech.apnafund.lambda.whatsapp.WhatsAppProcessorHandler::handleRequest"

  filename         = "${path.root}/../../artifacts/whatsapp-processor.jar"
  source_code_hash = filebase64sha256("${path.root}/../../artifacts/whatsapp-processor.jar")

  timeout     = 30
  memory_size = 256

  environment {
    variables = {
      META_WA_TOKEN          = var.meta_wa_token
      META_PHONE_NUMBER_ID  = var.meta_phone_number_id
    }
  }
}

resource "aws_lambda_event_source_mapping" "whatsapp_processor_sqs" {
  event_source_arn = module.notifications.whatsapp_queue_arn
  function_name    = aws_lambda_function.whatsapp_processor.arn
  batch_size                         = 10
  maximum_batching_window_in_seconds = 10
  enabled                            = true
}

resource "aws_iam_role_policy" "deploy_sns_publish_user_events" {
  name = "deploy-sns-publish-user-events"
  role = module.iam_deploy_role.role_name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = "sns:Publish"
        Resource = module.notifications.user_events_topic_arn
      }
    ]
  })
}

resource "aws_iam_role_policy" "deploy_sns_publish_support_events" {
  name = "deploy-sns-publish-support-events"
  role = module.iam_deploy_role.role_name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = "sns:Publish"
        Resource = module.notifications.support_events_topic_arn
      }
    ]
  })
}

module "assets" {
  source      = "../../modules/assets"
  bucket_name = "apnafund-assets"
  env         = "dev"
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
output "dev_eip" {
  value = aws_eip.dev_eip.public_ip
}
