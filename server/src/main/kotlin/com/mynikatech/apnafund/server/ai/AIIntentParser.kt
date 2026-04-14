package com.mynikatech.apnafund.server.ai

import com.mynikatech.apnafund.net.dto.AIEntity
import com.mynikatech.apnafund.net.dto.AIIntent
import com.mynikatech.apnafund.net.dto.AIIntentResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class AIIntentParser {

    fun parse(message: String): AIIntentResult {

        val msg = message.lowercase()

        return when {
            msg.contains("loan") -> AIIntentResult(
                intent = AIIntent.FETCH_DATA,
                entity = AIEntity.LOAN,
                filters = extractLoanFilters(msg).toJsonObject()
            )

            msg.contains("fund") -> AIIntentResult(
                intent = AIIntent.FETCH_DATA,
                entity = AIEntity.FUND
            )

            msg.contains("summary") -> AIIntentResult(
                intent = AIIntent.SUMMARY
            )

            else -> AIIntentResult(intent = AIIntent.UNKNOWN)
        }
    }

    private fun extractLoanFilters(msg: String): Map<String, String> {
        val filters = mutableMapOf<String, String>()

        val statusKeywords = listOf(
            "paid", "completed", "done", "closed",
            "pending", "unpaid", "due", "active"
        )

        statusKeywords.firstOrNull { msg.contains(it) }?.let {
            filters["status"] = it
        }

        // simple name detection (can improve later)
        if (msg.contains("kapil")) filters["borrower"] = "Kapil"

        return filters
    }
    fun Map<String, String>.toJsonObject(): JsonObject {
        return buildJsonObject {
            this@toJsonObject.forEach { (key, value) ->
                put(key, JsonPrimitive(value))
            }
        }
    }

}