package com.mynikatech.apnafund.server.ai


import com.mynikatech.apnafund.net.dto.*
import com.mynikatech.apnafund.server.users.UserFinanceService
import kotlinx.serialization.json.*

class DepositHandler(
    private val userFinanceService: UserFinanceService,
    private val responseBuilder: AIResponseBuilder
) {

    fun handle(
        intent: AIIntentResult,
        context: AIContext
    ): AIResponse {

        val deposits = getFilteredDeposits(context.userId, intent.filters)

        return when (intent.action) {

            AIAction.LIST -> buildDepositList(deposits, intent.filters)

            else -> generic()
        }
    }

    // =========================
    // 🔥 DATA LAYER
    // =========================

    private fun getFilteredDeposits(
        userId: Int,
        filters: JsonObject
    ): List<DepositsWithMemberNamesDto> {

        val deposits = userFinanceService.getDepositsForUser(userId)

        val depositorFilter = filters["depositor"]?.jsonPrimitive?.contentOrNull
        val monthFilter = filters["month"]?.jsonPrimitive?.contentOrNull
        val yearFilter = filters["year"]?.jsonPrimitive?.contentOrNull

        return deposits.filter { deposit ->


            val depositorMatch = depositorFilter?.let {
                deposit.firstName.contains(it, ignoreCase = true) ||
                        deposit.lastName.contains(it, ignoreCase = true)
            } ?: true

            val monthMatch = monthFilter?.let { input ->

                val inputMonth = normalizeMonth(input)
                val depositMonth = normalizeMonth(deposit.depositMonth)

                inputMonth != null && inputMonth == depositMonth

            } ?: true

            val yearMatch = yearFilter?.let {
                deposit.depositYear.toString() == it
            } ?: true

            depositorMatch && monthMatch && yearMatch
        }
    }

    private fun normalizeMonth(input: String?): Int? {
        if (input == null) return null

        return when (input.trim().lowercase()) {
            "jan", "january", "1", "01" -> 1
            "feb", "february", "2", "02" -> 2
            "mar", "march", "3", "03" -> 3
            "apr", "april", "4", "04" -> 4
            "may", "5", "05" -> 5
            "jun", "june", "6", "06" -> 6
            "jul", "july", "7", "07" -> 7
            "aug", "august", "8", "08" -> 8
            "sep", "september", "9", "09" -> 9
            "oct", "october", "10" -> 10
            "nov", "november", "11" -> 11
            "dec", "december", "12" -> 12
            else -> null
        }
    }

    // =========================
    // LIST
    // =========================

    private fun buildDepositList(
        deposits: List<DepositsWithMemberNamesDto>,
        filters: JsonObject
    ): AIResponse {

        if (deposits.isEmpty()) {
            return responseBuilder.buildEmptyResponse(AIEntity.DEPOSIT, filters)
        }

        return AIResponse(
            reply = "Here are your ${deposits.size} loans:",
            type = AIResponseType.TABLE,
            data = buildDepositsable(deposits),
            actions = listOf(
                AIActionItem("View Deposits", UIActionType.OPEN_DEPOSITS)
            )
        )
    }

    // =========================
    // TABLE BUILDER
    // =========================

    private fun buildDepositsable(
        deposit: List<DepositsWithMemberNamesDto>
    ): JsonElement {

        return buildJsonObject {

            put("columns", buildJsonArray {
                add(JsonPrimitive("Depositor"))
                add(JsonPrimitive("Deposited Date"))
                add(JsonPrimitive("Deposit Month"))
                add(JsonPrimitive("Deposit Year"))
                add(JsonPrimitive("Late Fee"))
            })

            put("rows", buildJsonArray {
                deposit.forEach {
                    add(buildJsonArray {
                        add(JsonPrimitive(it.fullName))
                        add(JsonPrimitive(it.depositedDate))
                        add(JsonPrimitive(it.depositMonth))
                        add(JsonPrimitive(it.depositYear))
                        add(JsonPrimitive(it.lateFee))
                    })
                }
            })
        }
    }

    // =========================
    // GENERIC
    // =========================

    private fun generic() = AIResponse(
        reply = "Unsupported deposit action. Please ask something else or check with Apna Fund Support",
        type = AIResponseType.TEXT
    )
}