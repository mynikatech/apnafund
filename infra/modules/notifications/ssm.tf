resource "aws_ssm_parameter" "sns_user_events_arn" {
  name  = "/apnafund/${var.env}/sns/user-events-arn"
  type  = "String"
  value = aws_sns_topic.user_events.arn
}

resource "aws_ssm_parameter" "sns_support_events_arn" {
  name  = "/apnafund/${var.env}/sns/support-events-arn"
  type  = "String"
  value = aws_sns_topic.support_events.arn
}