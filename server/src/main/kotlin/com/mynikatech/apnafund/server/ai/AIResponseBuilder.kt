package com.mynikatech.apnafund.server.ai

import com.mynikatech.apnafund.net.dto.AIActionItem
import com.mynikatech.apnafund.net.dto.AIEntity
import com.mynikatech.apnafund.net.dto.AIResponse
import com.mynikatech.apnafund.net.dto.AIResponseType
import com.mynikatech.apnafund.net.dto.UIActionType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class AIResponseBuilder {

    fun buildEmptyResponse(
        entity: AIEntity,
        filters: JsonObject
    ): AIResponse {

        val status = filters["status"]?.jsonPrimitive?.contentOrNull
        val borrower = filters["borrower"]?.jsonPrimitive?.contentOrNull

        val entityName = when (entity) {
            AIEntity.LOAN -> "loans"
            AIEntity.FUND -> "funds"
            AIEntity.GROUP -> "groups"
            AIEntity.DEPOSIT -> "deposits"
            AIEntity.LOAN_EMI -> "emis"
            else -> "items"
        }

        val message = buildString {
            append("You have no ")

            if (status != null) {
                append(status.lowercase())
                append(" ")
            }

            append(entityName)

            if (borrower != null) {
                append(" for $borrower")
            }

            append(".")
        }

        return AIResponse(
            reply = message,
            type = AIResponseType.EMPTY,
            data = buildJsonObject {
                put("entity", JsonPrimitive(entity.name))
                put("empty", JsonPrimitive(true))
            },
            actions = defaultActions(entity)
        )
    }

    fun defaultActions(entity: AIEntity): List<AIActionItem> {
        return when (entity) {

            AIEntity.LOAN -> listOf(
                AIActionItem("View All Loans", UIActionType.OPEN_LOANS),
                AIActionItem("Apply Loan", UIActionType.APPLY_LOAN)
            )

            AIEntity.FUND -> listOf(
                AIActionItem("View Funds", UIActionType.OPEN_FUNDS)
            )

            AIEntity.GROUP -> listOf(
                AIActionItem("View Groups", UIActionType.OPEN_GROUPS)
            )

            AIEntity.DEPOSIT -> listOf(
                AIActionItem("View Deposits", UIActionType.OPEN_DEPOSITS)
            )

            AIEntity.LOAN_EMI -> listOf(
                AIActionItem("View EMI", UIActionType.OPEN_EMI)
            )

            else -> emptyList()
        }
    }
}