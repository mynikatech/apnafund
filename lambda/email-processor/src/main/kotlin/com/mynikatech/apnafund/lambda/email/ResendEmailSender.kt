package com.mynikatech.apnafund.lambda.email

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory


class ResendEmailSender : EmailSender {

    private val client = OkHttpClient()
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun send(
        to: String,
        subject: String,
        body: String
    ) {

        val json = Json.encodeToString(
            ResendRequest(
                from = System.getenv("RESEND_FROM_EMAIL"),
                to = listOf(to),
                subject = subject,
                html = body
            )
        )
        logger.info("Sending email via RESEND to {}", to)

        val request = Request.Builder()
            .url("https://api.resend.com/emails")
            .addHeader(
                "Authorization",
                "Bearer ${System.getenv("RESEND_API_KEY")}"
            )
            .post(
                json.toRequestBody(
                    "application/json".toMediaType()
                )
            )
            .build()

        client.newCall(request).execute().use {
            if (!it.isSuccessful) {
                throw RuntimeException(
                    "Resend failed: ${it.code}"
                )
            }
        }
    }
}