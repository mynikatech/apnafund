package com.mynikatech.apnafund.lambda.email

import org.slf4j.LoggerFactory
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.*

class SesEmailSender: EmailSender{

    private val logger = LoggerFactory.getLogger(javaClass)

    private val ses = SesClient.builder()
        .region(Region.AP_SOUTH_1)
        .build()

    override fun send(to: String, subject: String, body: String) {

        val request = SendEmailRequest.builder()
            .destination(
                Destination.builder()
                    .toAddresses(to)
                    .build()
            )
            .message(
                Message.builder()
                    .subject(Content.builder().data(subject).build())
                    .body(
                        Body.builder()
                            .html(Content.builder().data(body).build())
                            .build()
                    )
                    .build()
            )
            .source(System.getenv("SES_FROM_EMAIL"))
            .build()
        logger.info("Sending email via SES to {}", to)

        ses.sendEmail(request)
    }
}
