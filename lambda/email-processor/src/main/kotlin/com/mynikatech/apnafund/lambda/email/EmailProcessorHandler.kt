package com.mynikatech.apnafund.lambda.email

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.mynikatech.apnafund.net.dto.Channel
import com.mynikatech.apnafund.net.dto.NotificationEvent
import com.mynikatech.apnafund.net.dto.SnsEnvelope
import com.mynikatech.apnafund.net.dto.SupportEvent
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class EmailProcessorHandler : RequestHandler<SQSEvent, Unit> {

    private val json = Json { ignoreUnknownKeys = true }
    private val sender = EmailSenderFactory.create()
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handleRequest(event: SQSEvent, context: Context) {
        event.records.forEach { record ->
            val envelope = json.decodeFromString<SnsEnvelope>(record.body)
            val messageJson = envelope.Message

            if (messageJson.contains("\"type\":\"SUPPORT_EMAIL\"")) {
                handleSupport(json.decodeFromString(messageJson))
            } else {
                val notificationEvent =
                    json.decodeFromString<NotificationEvent>(messageJson)

                log.info("Processing user email notification")
                handleUser(notificationEvent)
            }
        }
    }

    private fun handleUser(event: NotificationEvent) {
        if (!event.channels.contains(Channel.EMAIL)) return

        val (subject, html) = UserEmailRenderer.render(event)
        val email = event.email
            ?: error("EmailPayload missing for EMAIL channel")

        sender.send(
            to = email.to,
            subject = subject,
            body = html
        )
    }

    private fun handleSupport(event: SupportEvent) {
        val (subject, html) = SupportEmailRenderer.render(event)

        sender.send(
            to = System.getenv("SUPPORT_EMAIL_TO"),
            subject = subject,
            body = html
        )
    }
}