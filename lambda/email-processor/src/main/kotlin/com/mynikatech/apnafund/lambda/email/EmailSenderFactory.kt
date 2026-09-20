package com.mynikatech.apnafund.lambda.email

import org.slf4j.LoggerFactory

object EmailSenderFactory {

    private val logger = LoggerFactory.getLogger(EmailSenderFactory::class.java)

    private val sesSender by lazy {
        SesEmailSender()
    }

    private val resendSender by lazy {
        ResendEmailSender()
    }

    fun create(): EmailSender {

        val provider =
            System.getenv("EMAIL_PROVIDER")
                ?.uppercase()
                ?: "SES"

        logger.info("Using email provider: {}", provider)

        return when (provider) {
            "RESEND" -> resendSender
            else -> sesSender
        }
    }
}