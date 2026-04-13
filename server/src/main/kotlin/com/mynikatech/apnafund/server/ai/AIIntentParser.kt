package com.mynikatech.apnafund.server.ai

import com.mynikatech.apnafund.net.dto.AIEntity
import com.mynikatech.apnafund.net.dto.AIIntent
import com.mynikatech.apnafund.net.dto.AIIntentResult

class AIIntentParser {

    fun parse(message: String): AIIntentResult {

        val msg = message.lowercase()

        return when {
            msg.contains("loan") -> AIIntentResult(
                intent = AIIntent.FETCH_DATA,
                entity = AIEntity.LOAN,
                filters = extractLoanFilters(msg)
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

}