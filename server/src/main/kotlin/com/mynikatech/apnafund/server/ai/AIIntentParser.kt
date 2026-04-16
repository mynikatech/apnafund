package com.mynikatech.apnafund.server.ai

import com.mynikatech.apnafund.net.dto.AIAction
import com.mynikatech.apnafund.net.dto.AIEntity
import com.mynikatech.apnafund.net.dto.AIIntent
import com.mynikatech.apnafund.net.dto.AIIntentResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class AIIntentParser {

    fun parse(message: String): AIIntentResult {

        val msg = message.lowercase()

        if (isHelpQuery(msg)) {
            val entity = detectEntity(msg)
            val action = inferAction(msg, JsonObject(emptyMap()))

            return AIIntentResult(
                intent = AIIntent.HELP,
                entity = entity,
                filters = JsonObject(emptyMap()),
                action = action
            )
        }


        return when {

            // --------------------------
            // LOAN
            // --------------------------
            msg.contains("loan") -> {

                val filters = extractLoanFilters(msg).toJsonObject()
                val action = inferAction(msg, filters)

                AIIntentResult(
                    intent = AIIntent.FETCH_DATA,
                    entity = AIEntity.LOAN,
                    filters = filters,
                    action = action
                )
            }
            msg.contains("deposit") -> {

                val filters = extractFilters(msg).toJsonObject()
                val action = inferAction(msg, filters)

                AIIntentResult(
                    intent = AIIntent.FETCH_DATA,
                    entity = AIEntity.DEPOSIT,
                    filters = filters,
                    action = action
                )
            }

            msg.contains("emi") -> {

                val filters = extractFilters(msg).toJsonObject()
                val action = inferAction(msg, filters)

                AIIntentResult(
                    intent = AIIntent.FETCH_DATA,
                    entity = AIEntity.LOAN_EMI,
                    filters = filters,
                    action = action
                )
            }

            // --------------------------
            // FUND
            // --------------------------
            msg.contains("fund") -> {

                val filters = extractFundFilters(msg).toJsonObject()
                val action = inferAction(msg, filters)

                AIIntentResult(
                    intent = AIIntent.FETCH_DATA,
                    entity = AIEntity.FUND,
                    filters = filters,
                    action = action
                )
            }

            // --------------------------
            // SUMMARY
            // --------------------------
            msg.contains("summary") -> {
                AIIntentResult(
                    intent = AIIntent.SUMMARY,
                    action = AIAction.SUMMARY
                )
            }

            // --------------------------
            // DEFAULT
            // --------------------------
            else -> AIIntentResult(
                intent = AIIntent.UNKNOWN,
                action = AIAction.LIST // safe fallback
            )
        }
    }

    private fun isHelpQuery(msg: String): Boolean {
        return msg.contains("how") ||
                msg.contains("help") ||
                msg.contains("what is") ||
                msg.contains("how to")
    }

    private fun detectEntity(msg: String): AIEntity? {
        return when {
            msg.contains("loan") -> AIEntity.LOAN
            msg.contains("fund") -> AIEntity.FUND
            msg.contains("deposit") -> AIEntity.DEPOSIT
            msg.contains("group") -> AIEntity.GROUP
            else -> null
        }
    }

    // =========================
    // ACTION INFERENCE
    // =========================

    private fun inferAction(
        msg: String,
        filters: JsonObject
    ): AIAction {

        return when {
            msg.contains("add") || msg.contains("create") ->
                AIAction.CREATE

            msg.contains("where") || msg.contains("how") ->
                AIAction.NAVIGATE

            msg.contains("summary") -> AIAction.SUMMARY

            msg.contains("detail") -> AIAction.DETAILS

            msg.contains("member") -> AIAction.MEMBERS

            filters.containsKey("fundName") ||
                    filters.containsKey("loanId") ->
                AIAction.DETAILS

            else -> AIAction.LIST
        }
    }

    // =========================
    // LOAN FILTERS
    // =========================

    private fun extractLoanFilters(msg: String): Map<String, String> {

        val filters = mutableMapOf<String, String>()

        val statusKeywords = listOf(
            "paid", "completed", "done", "closed",
            "pending", "unpaid", "due", "active"
        )

        statusKeywords.firstOrNull { msg.contains(it) }?.let {
            filters["status"] = it
        }

        // simple name detection
        if (msg.contains("kapil")) {
            filters["borrower"] = "Kapil"
        }

        return filters
    }

    // =========================
    // FUND FILTERS
    // =========================

    private fun extractFundFilters(msg: String): Map<String, String> {

        val filters = mutableMapOf<String, String>()

        val statusKeywords = listOf(
            "active", "running",
            "closed", "completed", "matured"
        )

        statusKeywords.firstOrNull { msg.contains(it) }?.let {
            filters["status"] = it
        }

        // very basic name detection (can improve later)
        if (msg.contains("apna")) {
            filters["fundName"] = "Apna"
        }

        if (msg.contains("all")) {
            filters["scope"] = "ALL"
        }

        return filters
    }

    private fun extractFilters(msg: String): Map<String, String> {
        val filters = mutableMapOf<String, String>()
        val text = msg.lowercase()

        // ✅ Status
        val statusKeywords = mapOf(
            "active" to "ACTIVE",
            "running" to "ACTIVE",
            "closed" to "CLOSED",
            "completed" to "COMPLETED",
            "matured" to "MATURED",
            "pending" to "PENDING"
        )

        statusKeywords.entries.firstOrNull { text.contains(it.key) }?.let {
            filters["status"] = it.value
        }

        // ✅ Scope
        when {
            text.contains("all") -> filters["scope"] = "ALL"
            text.contains("my") -> filters["scope"] = "SELF"
        }

        // ✅ Month extraction (basic but useful)
        val months = listOf(
            "january","february","march","april","may","june",
            "july","august","september","october","november","december"
        )

        months.firstOrNull { text.contains(it) }?.let {
            filters["month"] = it.uppercase()
        }

        // ✅ Year extraction (simple regex)
        Regex("""\b(20\d{2})\b""").find(text)?.let {
            filters["year"] = it.value
        }

        return filters
    }

    // =========================
    // MAP → JSON
    // =========================

    private fun Map<String, String>.toJsonObject(): JsonObject {
        return buildJsonObject {
            this@toJsonObject.forEach { (key, value) ->
                put(key, JsonPrimitive(value))
            }
        }
    }
}