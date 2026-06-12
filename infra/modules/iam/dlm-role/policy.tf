resource "aws_iam_role_policy_attachment" "dlm_service_role" {
  role       = aws_iam_role.this.name

  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSDataLifecycleManagerServiceRole"
}