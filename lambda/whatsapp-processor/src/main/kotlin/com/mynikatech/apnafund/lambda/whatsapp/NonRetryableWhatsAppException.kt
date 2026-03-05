package com.mynikatech.apnafund.lambda.whatsapp

class NonRetryableWhatsAppException(
    val status: Int,
    message: String?
) : RuntimeException(message)