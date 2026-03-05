resource "aws_sqs_queue" "email_dlq" {
  name = "${var.app_name}-${var.env}-email-dlq"
}

resource "aws_sqs_queue" "email_queue" {
  name = "${var.app_name}-${var.env}-email-queue"

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.email_dlq.arn
    maxReceiveCount     = 5
  })
}

resource "aws_sqs_queue" "whatsapp_dlq" {
  name = "${var.app_name}-${var.env}-whatsapp-dlq"
}

resource "aws_sqs_queue" "whatsapp_queue" {
  name = "${var.app_name}-${var.env}-whatsapp-queue"

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.whatsapp_dlq.arn
    maxReceiveCount     = 5
  })
}

resource "aws_sqs_queue" "support_dlq" {
  name = "${var.app_name}-${var.env}-support-dlq"
}


resource "aws_sqs_queue" "support_email_queue" {
  name = "${var.app_name}-${var.env}-support-email-queue"

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.support_dlq.arn
    maxReceiveCount     = 5
  })
}

data "aws_iam_policy_document" "sns_to_sqs" {
  statement {
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["sns.amazonaws.com"]
    }

    actions   = ["sqs:SendMessage"]
    resources = [
      aws_sqs_queue.email_queue.arn,
      aws_sqs_queue.whatsapp_queue.arn,
      aws_sqs_queue.support_email_queue.arn
    ]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values = [
        aws_sns_topic.user_events.arn,
        aws_sns_topic.support_events.arn
      ]
    }
  }
}

resource "aws_sqs_queue_policy" "allow_sns" {
  queue_url = aws_sqs_queue.email_queue.id
  policy    = data.aws_iam_policy_document.sns_to_sqs.json
}

resource "aws_sqs_queue_policy" "allow_sns_whatsapp" {
  queue_url = aws_sqs_queue.whatsapp_queue.id
  policy    = data.aws_iam_policy_document.sns_to_sqs.json
}

resource "aws_sqs_queue_policy" "allow_sns_support" {
  queue_url = aws_sqs_queue.support_email_queue.id
  policy    = data.aws_iam_policy_document.sns_to_sqs.json
}


