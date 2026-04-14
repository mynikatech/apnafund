package com.mynikatech.apnafund.server.ai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory

class GeminiClient(
    private val apiKey: String
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun generateContent(
        prompt: String,
        userMessage: String
    ): String {

        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val requestBody = buildJsonObject {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject {
                            put("text", prompt)
                        })
                        add(buildJsonObject {
                            put("text", userMessage)
                        })
                    })
                })
            })
        }

        val rawResponse: String = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }.body()

        log.info("GEMINI RAW RESPONSE: $rawResponse")

        return parseResponse(rawResponse)
    }

    private fun parseResponse(raw: String): String {

        val element = json.parseToJsonElement(raw)
        val obj = element.jsonObject

        // Handle error
        if ("error" in obj) {
            val message = obj["error"]
                ?.jsonObject
                ?.get("message")
                ?.jsonPrimitive
                ?.content

            throw Exception("Gemini API Error: $message")
        }

        // Extract text
        return obj["candidates"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("content")
            ?.jsonObject
            ?.get("parts")
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("text")
            ?.jsonPrimitive
            ?.content
            ?: throw Exception("Empty Gemini response")
    }
}