package com.mynikatech.apnafund.server.ai

import com.mynikatech.apnafund.net.dto.AIEntity

object StatusMapper {

    fun map(entity: AIEntity, input: String): List<String> {

        val normalized = input.lowercase()

        return when (entity) {

            AIEntity.LOAN -> mapLoanStatus(normalized)

            AIEntity.FUND -> mapFundStatus(normalized)

            AIEntity.DEPOSIT -> mapDepositStatus(normalized)

            AIEntity.LOAN_EMI -> mapEmiStatus(normalized)

            else -> listOf(input.uppercase())
        }
    }

    private fun mapLoanStatus(input: String): List<String> {

        val value = input.lowercase()

        return when (value) {

            // OPEN group
            "open", "active", "running", "ongoing" ->
                listOf("OPEN", "ACTIVE")

            // CLOSED group
            "closed", "completed", "finished", "settled",
            "paid", "cleared", "ended", "over" ->
                listOf("CLOSED")

            // PENDING group
            "pending", "due", "unpaid" ->
                listOf("PENDING")

            else -> listOf(input.uppercase())
        }
    }

    private fun mapFundStatus(input: String): List<String> {

        val value = input.lowercase()

        return when (value) {

            // ACTIVE group
            "active", "running", "ongoing" ->
                listOf("ACTIVE")

            // CLOSED group
            "closed", "completed", "matured", "finished",
            "ended", "inactive" ->
                listOf("CLOSED")

            else -> listOf(input.uppercase())
        }
    }

    private fun mapDepositStatus(input: String): List<String> {
        return when (input) {
            "paid" -> listOf("PAID")
            "pending" -> listOf("PENDING")
            else -> listOf(input.uppercase())
        }
    }

    private fun mapEmiStatus(input: String): List<String> {
        return when (input) {
            "paid" -> listOf("PAID")
            "due" -> listOf("DUE")
            else -> listOf(input.uppercase())
        }
    }
    fun mapStatus(input: String): List<String> {

        val value = input.trim().lowercase()

        return when (value) {

            // CLOSED group
            "closed", "completed", "done", "finished",
            "settled", "paid", "cleared",
            "matured", "ended", "over", "inactive" ->
                listOf("CLOSED")

            // PENDING group
            "pending", "unpaid", "due", "remaining" ->
                listOf("PENDING")

            else -> listOf(input.uppercase())
        }
    }
}