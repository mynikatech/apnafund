package com.mynikatech.apnafund.server.ai

import com.mynikatech.apnafund.net.dto.AIAction
import com.mynikatech.apnafund.net.dto.AIEntity
import com.mynikatech.apnafund.net.dto.AIIntent
import com.mynikatech.apnafund.net.dto.AIIntentResult
import com.mynikatech.apnafund.net.dto.AIProviderType
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

class AIClient(
    private val openAIClient: OpenAIClient?,
    private val geminiClient: GeminiClient?,
    private val provider: AIProviderType = AIProviderType.OPENAI,
    private val enableFallback: Boolean = true
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun getIntentFromAI(message: String): AIIntentResult {

        log.info("AI Provider: $provider")
        log.info("User message: $message")

        val prompts = listOf(
            selectPrompt(message),   // first try (dynamic)
            standardPrompt(),        // fallback
            fullPrompt()             // final fallback
        ).distinct()

        for ((index, prompt) in prompts.withIndex()) {

            try {
                log.info("Attempt ${index + 1} with prompt size: ${prompt.length}")

                val raw = callProvider(prompt, message)

                log.info("Raw AI response: $raw")

                val cleanJson = raw
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()

                if (!cleanJson.startsWith("{")) {
                    throw Exception("Invalid JSON from AI")
                }

                val obj = jsonParser.parseToJsonElement(cleanJson).jsonObject

                val intent = obj["intent"]?.jsonPrimitive?.content?.let {
                    runCatching { AIIntent.valueOf(it) }.getOrElse { AIIntent.UNKNOWN }
                } ?: AIIntent.UNKNOWN

                val entity = obj["entity"]?.jsonPrimitive?.content?.let {
                    runCatching { AIEntity.valueOf(it) }.getOrNull()
                }
                val action = obj["action"]?.jsonPrimitive?.content?.let {
                    runCatching { AIAction.valueOf(it) }.getOrNull()
                }
                val finalAction = action ?: inferAction(intent, entity)
                val filters = obj["filters"]?.jsonObject ?: JsonObject(emptyMap())

                // SUCCESS → return immediately
                return AIIntentResult(
                    intent = intent,
                    entity = entity,
                    filters = filters,
                    action = finalAction
                )

            } catch (e: Exception) {

                log.warn("Attempt ${index + 1} failed: ${e.message}")

                // try next prompt
            }
        }

        // All prompts failed → let service fallback handle it
        throw Exception("AI failed for all prompt levels")
    }
    private fun inferAction(
        intent: AIIntent,
        entity: AIEntity?
    ): AIAction {

        return when (intent) {

            AIIntent.FETCH_DATA -> AIAction.LIST

            AIIntent.SUMMARY -> AIAction.SUMMARY

            else -> AIAction.LIST // safe fallback
        }
    }

    private suspend fun callProvider(prompt: String, message: String): String {

        return try {
            retry {

                when (provider) {

                    AIProviderType.OPENAI -> {
                        openAIClient?.chatCompletion(
                            model = "gpt-4o-mini",
                            messages = listOf(
                                systemMessage(prompt),
                                userMessage(message)
                            )
                        )?.choices?.firstOrNull()?.message?.content
                            ?: throw Exception("Empty OpenAI response")
                    }

                    AIProviderType.GEMINI -> {
                        geminiClient?.generateContent(prompt, message)
                            ?: throw Exception("Gemini client not configured")
                    }
                }
            }

        } catch (e: Exception) {

            log.error("Primary provider failed: ${e.message}")

            if (!enableFallback) throw e

            log.info("Trying fallback provider...")

            return retry {
                when (provider) {

                    AIProviderType.OPENAI -> {
                        geminiClient?.generateContent(prompt, message)
                            ?: throw Exception("Fallback Gemini not available")
                    }

                    AIProviderType.GEMINI -> {
                        openAIClient?.chatCompletion(
                            model = "gpt-4o-mini",
                            messages = listOf(
                                systemMessage(prompt),
                                userMessage(message)
                            )
                        )?.choices?.firstOrNull()?.message?.content
                            ?: throw Exception("Fallback OpenAI failed")
                    }
                }
            }
        }
    }


    private suspend fun retry(times: Int = 1, block: suspend () -> String): String {
        repeat(times) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                if (attempt == times - 1) throw e
                delay(300L * (attempt + 1)) // small backoff
            }
        }
        throw Exception("Retry failed")
    }

    private fun selectPrompt(message: String): String {
        val words = message.trim().split("\\s+".toRegex()).size

        return when {
            words <= 4 -> shortPrompt()
            words <= 8 -> standardPrompt()
            else -> fullPrompt()
        }
    }
}