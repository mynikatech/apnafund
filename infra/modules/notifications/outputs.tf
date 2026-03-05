output "sns_topic_arns" {
  value = [
    aws_sns_topic.user_events.arn,
    aws_sns_topic.support_events.arn
  ]
}

output "queues" {
  value = {
    email    = aws_sqs_queue.email_queue.url
    whatsapp = aws_sqs_queue.whatsapp_queue.url
    support  = aws_sqs_queue.support_email_queue.url
  }
}
output "email_queue_arn" {
  value = aws_sqs_queue.email_queue.arn
}

output "whatsapp_queue_arn" {
  value = aws_sqs_queue.whatsapp_queue.arn
}

output "email_queue_url" {
  value = aws_sqs_queue.email_queue.id
}

output "user_events_topic_arn" {
  value = aws_sns_topic.user_events.arn
}

output "support_events_topic_arn" {
  value = aws_sns_topic.support_events.arn
}

