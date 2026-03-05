resource "aws_sns_topic_subscription" "user_email" {
  topic_arn = aws_sns_topic.user_events.arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.email_queue.arn
  filter_policy = jsonencode({
    channel = ["EMAIL"]
  })
}

resource "aws_sns_topic_subscription" "user_whatsapp" {
  topic_arn = aws_sns_topic.user_events.arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.whatsapp_queue.arn
  filter_policy = jsonencode({
    channel = ["WHATSAPP"]
  })
}

resource "aws_sns_topic_subscription" "support_email" {
  topic_arn = aws_sns_topic.support_events.arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.support_email_queue.arn

}
