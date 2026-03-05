do {
    $resp = aws sqs receive-message `
    --profile ApnaFundAdmin `
    --region ap-south-1 `
    --queue-url https://sqs.ap-south-1.amazonaws.com/861082243595/apnafund-dev-email-queue `
    --max-number-of-messages 10 `
    --wait-time-seconds 2 | ConvertFrom-Json

    if ($resp.Messages) {
        foreach ($msg in $resp.Messages) {
            aws sqs delete-message `
        --profile ApnaFundAdmin `
        --region ap-south-1 `
        --queue-url https://sqs.ap-south-1.amazonaws.com/861082243595/apnafund-dev-email-queue `
        --receipt-handle $msg.ReceiptHandle
        }
    }
} while ($resp.Messages)