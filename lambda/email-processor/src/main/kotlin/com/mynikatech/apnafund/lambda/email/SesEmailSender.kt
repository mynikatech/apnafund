package com.mynikatech.apnafund.lambda.email

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.*

class SesEmailSender {

    private val ses = SesClient.builder()
        .region(Region.AP_SOUTH_1)
        .build()

    fun send(to: String, subject: String, body: String) {

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

        ses.sendEmail(request)
    }
}
