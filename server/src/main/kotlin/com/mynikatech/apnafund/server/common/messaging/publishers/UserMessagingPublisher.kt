package com.mynikatech.apnafund.server.common.messaging.publishers

import com.mynikatech.apnafund.net.dto.NotificationEvent
import com.mynikatech.apnafund.server.common.messaging.AwsClients
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest

class UserMessagingPublisher(
    private val userEventsArn: String
) {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun publish(event: NotificationEvent) {
        event.channels.forEach { channel ->
            val request = PublishRequest.builder()
                .topicArn(userEventsArn)
                .message(json.encodeToString(event))
                .messageAttributes(
                    mapOf(
                        "channel" to MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue(channel.name)
                            .build()
                    )
                )
                .build()

            AwsClients.sns.publish(request)
        }
    }
}
