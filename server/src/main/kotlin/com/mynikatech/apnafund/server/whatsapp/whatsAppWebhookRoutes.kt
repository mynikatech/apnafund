package com.mynikatech.apnafund.server.whatsapp

import com.mynikatech.apnafund.net.dto.MetaWebhookDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.json.Json

fun Route.whatsAppWebhookRoutes(
    whatsAppMessagesSql: WhatsAppMessagesSql
) {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    route("/whatsapp/webhook") {

        get {

            val mode =
                call.request.queryParameters["hub.mode"]

            val token =
                call.request.queryParameters["hub.verify_token"]

            val challenge =
                call.request.queryParameters["hub.challenge"]

            if (
                mode == "subscribe" &&
                token ==
                (System.getenv("META_WEBHOOK_VERIFY_TOKEN")
                    ?: error("META_WEBHOOK_VERIFY_TOKEN not set"))

            ) {

                call.respondText(
                    challenge ?: ""
                )

            } else {

                call.respond(
                    HttpStatusCode.Forbidden
                )
            }
        }

        post {
            try {
                val payload =
                    call.receiveText()

                call.application.log.info(
                    "WhatsApp webhook received: {}",
                    payload
                )
                call.application.log.info(
                    "Responding OK to Keta"
                )

                call.respond(
                    HttpStatusCode.OK
                )


                val webhook =
                    json.decodeFromString<MetaWebhookDto>(payload)

                webhook.entry
                    .flatMap { it.changes }
                    .flatMap { it.value.statuses }
                    .forEach { status ->
                        whatsAppMessagesSql.upsertWhatsAppMessage(
                            userId = null,
                            eventType = null,
                            templateName = null,
                            phoneNumber = status.recipient_id,
                            metaMessageId = status.id,
                            waId = status.recipient_id,
                            status = status.status.uppercase(),
                            rawResponse = payload
                        )
                    }
            } catch (e: Exception) {

                call.application.log.error(
                    "WhatsApp webhook failed",
                    e
                )
                call.respond(
                    HttpStatusCode.OK
                )
            }
        }
    }
}
