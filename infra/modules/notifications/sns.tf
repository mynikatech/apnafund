resource "aws_sns_topic" "user_events" {
  name = "${var.app_name}-${var.env}-user-events"
}

resource "aws_sns_topic" "support_events" {
  name = "${var.app_name}-${var.env}-support-events"
}


