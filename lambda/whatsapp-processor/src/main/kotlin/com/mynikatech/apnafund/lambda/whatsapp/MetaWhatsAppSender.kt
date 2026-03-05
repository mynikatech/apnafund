package com.mynikatech.apnafund.lambda.whatsapp

import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import org.slf4j.LoggerFactory

class MetaWhatsAppSender {

    private val log = LoggerFactory.getLogger(javaClass)

    private val token = System.getenv("META_WA_TOKEN")
        ?: error("META_WA_TOKEN not set")

    private val phoneNumberId = System.getenv("META_PHONE_NUMBER_ID")
        ?: error("META_PHONE_NUMBER_ID not set")

    fun sendText(to: String, message: String) {
        val url = URL("https://graph.facebook.com/v19.0/$phoneNumberId/messages")
        val conn = url.openConnection() as HttpURLConnection

        conn.requestMethod = "POST"
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.doOutput = true

        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Content-Type", "application/json")

        val safeMessage = jsonEscape(message)

        val payload = """
        {
          "messaging_product": "whatsapp",
          "to": "$to",
          "type": "text",
          "text": {
            "body": "$safeMessage"
          }
        }
        """.trimIndent()

        conn.outputStream.use {
            it.write(payload.toByteArray(StandardCharsets.UTF_8))
        }

        val code = conn.responseCode
        val response = when {
            code in 200..299 -> conn.inputStream
            else -> conn.errorStream
        }?.bufferedReader()?.readText()

        if (code !in 200..299) {
            log.error("WhatsApp send failed | to={} | status={} | response={}", to, code, response)

            // Retryable vs non-retryable
            if (code in listOf(400, 401, 403, 404)) {
                // permanent failure → do NOT retry forever
                throw NonRetryableWhatsAppException(code, response)
            }

            error("WhatsApp send failed: $code")
        }

        log.info("WhatsApp message sent | to={} | response={}", to, response)
    }

    private fun jsonEscape(input: String): String =
        input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "")
}
