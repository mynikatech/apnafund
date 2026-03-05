package com.mynikatech.apnafund.server.common.messaging.publishers

import com.mynikatech.apnafund.net.dto.SupportEvent
import com.mynikatech.apnafund.server.common.messaging.AwsClients
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import software.amazon.awssdk.services.sns.model.PublishRequest

class SupportMessagingPublisher(
    private val supportEventsArn: String
) {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun publish(event: SupportEvent) {
        val messageJson = json.encodeToString(event)

        val request = PublishRequest.builder()
            .topicArn(supportEventsArn)
            .message(messageJson)
            .build()

        AwsClients.sns.publish(request)
    }
}