package com.mynikatech.apnafund.server.ai

import com.mynikatech.apnafund.net.dto.AIAction
import com.mynikatech.apnafund.net.dto.AIActionItem
import com.mynikatech.apnafund.net.dto.AIContext
import com.mynikatech.apnafund.net.dto.AIEntity
import com.mynikatech.apnafund.net.dto.AIIntentResult
import com.mynikatech.apnafund.net.dto.AIResponse
import com.mynikatech.apnafund.net.dto.AIResponseType
import com.mynikatech.apnafund.net.dto.FundWithDetailsDto
import com.mynikatech.apnafund.net.dto.UIActionType
import com.mynikatech.apnafund.server.users.UserFinanceService
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class FundHandler(
    private val userFinanceService: UserFinanceService,
    private val responseBuilder: AIResponseBuilder
) {

    fun handle(
        intent: AIIntentResult,
        context: AIContext
    ): AIResponse {

        val funds = getFilteredFunds(context.userId, intent.filters, context)
        val filters  = intent.filters

        return when (intent.action) {

            AIAction.LIST -> buildFundList(funds, filters)

            AIAction.SUMMARY -> buildFundSummary(funds, filters)

            AIAction.MEMBERS -> buildFundMembers(funds, filters)

            AIAction.DETAILS -> buildFundDetails(funds, filters)

            AIAction.CREATE -> AIResponse(
                reply = "You can add a new fund from the Funds screen.",
                type = AIResponseType.TEXT,
                actions = listOf(
                    AIActionItem("Add Fund", UIActionType.OPEN_ADD_FUND)
                )
            )

            AIAction.NAVIGATE -> AIResponse(
                reply = "Go to the Funds section to add a new fund.",
                type = AIResponseType.TEXT,
                actions = listOf(
                    AIActionItem("Open Funds", UIActionType.OPEN_FUNDS)
                )
            )

            else -> generic()
        }
    }

    private fun getFilteredFunds(
        userId: Int,
        filters: JsonObject,
        context: AIContext
    ): List<FundWithDetailsDto> {

        val allFunds = userFinanceService.getFundsForUser(userId)

        val fundName = filters["fundName"]?.jsonPrimitive?.contentOrNull
        val scope = filters["scope"]?.jsonPrimitive?.contentOrNull
        val statusFilter = filters["status"]?.jsonPrimitive?.contentOrNull


        val resolvedFunds = when {

            fundName != null -> {
                val inputs = fundName
                    .split(",", "&")
                    .map { it.trim().lowercase() }

                allFunds.filter { fund ->
                    inputs.any { input ->
                        fund.fundName.lowercase().contains(input) ||
                                fund.fundCode.lowercase().contains(input)
                    }
                }
            }

            scope == "ALL" -> allFunds

            context.activeFundId != null -> {
                allFunds.filter { it.fundId == context.activeFundId }
            }

            else -> allFunds
        }

        // 🔹 STEP 2: Filter (status)
        return resolvedFunds.filter { fund ->

            val statusMatch = statusFilter?.let { input ->
                StatusMapper.map(AIEntity.FUND, input).any {
                    fund.fundStatus.equals(it, ignoreCase = true)
                }
            } ?: true

            statusMatch
        }
    }

    // --------------------------
    // LIST
    // --------------------------

    private fun buildFundList(
        funds: List<FundWithDetailsDto>,
        filters: JsonObject
    ): AIResponse {

        if (funds.isEmpty()) {
            return responseBuilder.buildEmptyResponse(AIEntity.FUND, filters)
        }

        return AIResponse(
            reply = "Here are your ${funds.size} fund(s):",
            type = AIResponseType.TABLE,
            data = buildTable(funds),
            actions = listOf(
                AIActionItem("View Funds", UIActionType.OPEN_FUNDS)
            )
        )
    }

    // --------------------------
    // SUMMARY
    // --------------------------

    private fun buildFundSummary(
        funds: List<FundWithDetailsDto>,
        filters: JsonObject
    ): AIResponse {

        if (funds.isEmpty()) {
            return responseBuilder.buildEmptyResponse(AIEntity.FUND, filters)
        }

        val totalExpected = funds.sumOf { it.totalExpectedDeposit }
        val totalCurrent = funds.sumOf { it.totalCurrAmount }

        val data = buildJsonObject {
            put("fundCount", funds.size)
            put("totalExpectedAmount", totalExpected)
            put("totalCurrentAmount", totalCurrent)
        }

        return AIResponse(
            reply = "You are part of ${funds.size} fund(s). Current corpus is ₹$totalCurrent, against expected ₹$totalExpected.",
            type = AIResponseType.SUMMARY,
            data = data,
            actions = listOf(
                AIActionItem(
                    label = "View Funds",
                    type = UIActionType.OPEN_FUNDS
                )
            )
        )
    }

    // --------------------------
    // DETAILS
    // --------------------------

    private fun buildFundDetails(
        funds: List<FundWithDetailsDto>,
        filters: JsonObject
    ): AIResponse {

        if (funds.isEmpty()) {
            return responseBuilder.buildEmptyResponse(AIEntity.FUND, filters)
        }

        // 👉 pick first fund (or enhance later)
        val fund = funds.first()

        val data = buildJsonObject {
            put("fundName", JsonPrimitive(fund.fundName))
            put("expectedAmount", JsonPrimitive(fund.totalExpectedDeposit))
            put("currentAmount", JsonPrimitive(fund.totalCurrAmount))
        }

        return AIResponse(
            reply = "Here are the details of ${fund.fundName}:",
            type = AIResponseType.SUMMARY,
            data = data
        )
    }


    // --------------------------
    // COMMON HELPERS
    // --------------------------

    private fun buildTable(funds: List<FundWithDetailsDto>): JsonElement {
        return buildJsonObject {

            put("columns", buildJsonArray {
                add(JsonPrimitive("Fund Name"))
                add(JsonPrimitive("Expected Amount"))
                add(JsonPrimitive("Current Amount"))
                add(JsonPrimitive("End Date"))

            })

            put("rows", buildJsonArray {
                for (fund in funds) {
                    add(buildJsonArray {
                        add(JsonPrimitive(fund.fundName))
                        add(JsonPrimitive(fund.totalExpectedDeposit))
                        add(JsonPrimitive(fund.totalCurrAmount))
                        add(JsonPrimitive(fund.fundMaturityDate))
                    })
                }
            })
        }
    }

    private fun buildFundMembers(
        funds: List<FundWithDetailsDto>,
        filters: JsonObject
    ): AIResponse {

        if (funds.isEmpty()) {
            return responseBuilder.buildEmptyResponse(AIEntity.FUND, filters)
        }

        val members = funds.flatMap { fund ->
            userFinanceService.getFundMembersWithNamesForFund(fund.fundId)
        }

        if (members.isEmpty()) {
            return responseBuilder.buildEmptyResponse(AIEntity.FUND, filters)
        }

        val fundNames = funds.joinToString { it.fundName }

        return AIResponse(
            reply = "Here are the members of $fundNames:",
            type = AIResponseType.TABLE,
            data = buildJsonObject {
                put("columns", buildJsonArray {

                    add(JsonPrimitive("Name"))
                    add(JsonPrimitive("Joining Date"))
                })
                put("rows", buildJsonArray {
                    members.forEach {
                        add(buildJsonArray {
                            add(JsonPrimitive("${it.firstName} ${it.lastName}"))
                            add(JsonPrimitive(it.joiningDate))

                        })
                    }
                })
            }
        )
    }

    private fun generic() = AIResponse(
        reply = "I didn’t quite get that. Try asking things like:\\n• Show my funds\\n• Show fund members \\n• Show fund summary.\"",
        type = AIResponseType.TEXT
    )
}