package com.mynikatech.apnafund.lambda.whatsapp

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.mynikatech.apnafund.net.dto.Channel
import com.mynikatech.apnafund.net.dto.NotificationEvent
import com.mynikatech.apnafund.net.dto.SnsEnvelope
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class WhatsAppProcessorHandler : RequestHandler<SQSEvent, Unit> {

    private val log = LoggerFactory.getLogger(javaClass)

    private val json = Json { ignoreUnknownKeys = true }
    private val sender = MetaWhatsAppSender()

    override fun handleRequest(event: SQSEvent, context: Context) {

        event.records.forEach { record ->

            try {
                // Decode SNS envelope
                val sns = json.decodeFromString<SnsEnvelope>(record.body)

                // Decode actual NotificationEvent
                val notification =
                    json.decodeFromString<NotificationEvent>(sns.Message)

                if (Channel.WHATSAPP !in notification.channels) {
                    log.debug("Skipping non-whatsapp event {}", notification.eventType)
                    return@forEach
                }

                val wa = notification.whatsapp
                    ?: throw IllegalStateException(
                        "WhatsApp payload missing for ${notification.eventType}"
                    )

                val message = WhatsAppRenderer.render(notification)
                val template = wa.template
                val result =
                    if (!template.isNullOrBlank()) {

                        sender.sendTemplate(
                            to = wa.phone,
                            templateName = template,
                            params = wa.templateParams
                        )

                    } else {

                        sender.sendText(
                            to = wa.phone,
                            message = message
                        )
                    }

                log.info(
                    "WhatsApp sent eventType={} userId={} messageId={} rawResponse={}",
                    notification.eventType,
                    notification.userId,
                    result.metaMessageId,
                    result.rawResponse
                )

            } catch (e: NonRetryableWhatsAppException) {
                // IMPORTANT: swallow → message is ACKed
                log.error("Non-retryable WhatsApp error, skipping message: {}", e.message)

            } catch (e: Exception) {
                // Retryable → let Lambda fail
                log.error("WhatsApp processing failed, will retry", e)
                throw e
            }
        }
    }
}