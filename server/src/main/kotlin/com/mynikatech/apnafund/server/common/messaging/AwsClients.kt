package com.mynikatech.apnafund.server.common.messaging

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sqs.SqsClient

object AwsClients {

    val sns: SnsClient = SnsClient.builder()
        .region(Region.AP_SOUTH_1)
        .build()

    val sqs: SqsClient = SqsClient.builder()
        .region(Region.AP_SOUTH_1)
        .build()

    val ses: SesClient = SesClient.builder()
        .region(Region.AP_SOUTH_1)
        .build()
}