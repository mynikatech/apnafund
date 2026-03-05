# Wait a bit after EC2 creation to give SSM Agent time to register
resource "time_sleep" "wait_for_ssm" {
  depends_on      = [aws_instance.dev_server]
  create_duration = "30s"
}

# Output the EC2 instance ID — you can use this directly with SSM start-session
output "dev_instance_id" {
  description = "EC2 instance ID for the dev server (use with SSM start-session)"
  value       = aws_instance.dev_server.id
}

# (Optional) Friendly hint: where to find it in the console
output "dev_instance_name" {
  value = aws_instance.dev_server.tags["Name"]
}

# If you want Terraform to *fail* when SSM isn't ready, uncomment this block.
# It runs only on apply (not plan) and requires AWS CLI on your machine.
#
# resource "null_resource" "assert_ssm_online" {
#   depends_on = [time_sleep.wait_for_ssm]
#
#   provisioner "local-exec" {
#     when    = "create"
#     command = <<-EOT
#       aws ssm describe-instance-information --region ${var.aws_region} --profile ${var.aws_profile} \
#         --query "InstanceInformationList[?InstanceId=='${aws_instance.dev_server.id}'].PingStatus" --output text | findstr /C:"Online"
#       if %ERRORLEVEL% neq 0 (echo SSM agent not online yet && exit /b 1)
#     EOT
#     interpreter = ["cmd", "/C"]
#   }
# }