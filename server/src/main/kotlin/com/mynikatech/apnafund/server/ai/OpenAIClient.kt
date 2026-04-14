package com.mynikatech.apnafund.server.ai

import com.mynikatech.apnafund.net.dto.ChatMessageRequest
import com.mynikatech.apnafund.net.dto.OpenAIRequest
import com.mynikatech.apnafund.net.dto.OpenAIResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json


import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

class OpenAIClient(
    private val apiKey: String
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }
    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun chatCompletion(
        model: String,
        messages: List<ChatMessageRequest>
    ): OpenAIResponse {

        val responseText: String = client.post("https://api.openai.com/v1/chat/completions") {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)

            setBody(
                OpenAIRequest(
                    model = model,
                    messages = messages,
                    temperature = 0.0
                )
            )
        }.body()

        return parseResponse(responseText)
    }

    private fun parseResponse(raw: String): OpenAIResponse {

        log.info("OPENAI RAW RESPONSE: $raw")

        val element = json.parseToJsonElement(raw)
        val obj = element.jsonObject

        // Handle API error FIRST
        if ("error" in obj) {
            val message = obj["error"]
                ?.jsonObject
                ?.get("message")
                ?.jsonPrimitive
                ?.content

            log.error("OpenAI API Error: $message")
            throw Exception("OpenAI API Error: $message")
        }

        // Extra safety: ensure choices exist
        if ("choices" !in obj) {
            log.error("Invalid OpenAI response (missing choices): $raw")
            throw Exception("Invalid OpenAI response: missing 'choices'")
        }

        // Safe success parsing
        return json.decodeFromJsonElement(
            OpenAIResponse.serializer(),
            element
        )
    }
}