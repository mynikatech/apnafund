package com.mynikatech.apnafund.server.ai

import com.mynikatech.apnafund.net.dto.AIAction
import com.mynikatech.apnafund.net.dto.AIActionItem
import com.mynikatech.apnafund.net.dto.AIContext
import com.mynikatech.apnafund.net.dto.AIEntity
import com.mynikatech.apnafund.net.dto.AIIntentResult
import com.mynikatech.apnafund.net.dto.AIResponse
import com.mynikatech.apnafund.net.dto.AIResponseType
import com.mynikatech.apnafund.net.dto.LoanEmiWithMemberNamesDto
import com.mynikatech.apnafund.net.dto.UIActionType
import com.mynikatech.apnafund.server.users.UserFinanceService
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class LoanEMIHandler(
    private val userFinanceService: UserFinanceService,
    private val responseBuilder: AIResponseBuilder
) {

    fun handle(
        intent: AIIntentResult,
        context: AIContext
    ): AIResponse {

        val emis = getFilteredLoanEmis(context.userId, intent.filters)

        return when (intent.action) {

            AIAction.LIST -> buildLoanEMIList(emis, intent.filters)

            else -> generic()
        }
    }

    // =========================
    // 🔥 DATA LAYER
    // =========================

    private fun getFilteredLoanEmis(
        userId: Int,
        filters: JsonObject
    ): List<LoanEmiWithMemberNamesDto> {

        val emis = userFinanceService.getLoanEMIsForUser(userId)

        val depositorFilter = filters["depositor"]?.jsonPrimitive?.contentOrNull
        val monthFilter = filters["month"]?.jsonPrimitive?.contentOrNull
        val yearFilter = filters["year"]?.jsonPrimitive?.contentOrNull

        return emis.filter { emi ->


            val depositorMatch = depositorFilter?.let {
                emi.firstName.contains(it, ignoreCase = true) ||
                        emi.lastName.contains(it, ignoreCase = true)
            } ?: true

            val monthMatch = monthFilter?.let { input ->

                val inputMonth = normalizeMonth(input)
                val emiMonth = normalizeMonth(emi.emiMonth)

                inputMonth != null && inputMonth == emiMonth

            } ?: true

            val yearMatch = yearFilter?.let {
                emi.emiYear.toString() == it
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

    private fun buildLoanEMIList(
        emis: List<LoanEmiWithMemberNamesDto>,
        filters: JsonObject
    ): AIResponse {

        if (emis.isEmpty()) {
            return responseBuilder.buildEmptyResponse(AIEntity.DEPOSIT, filters)
        }

        return AIResponse(
            reply = "Here are your ${emis.size} loans:",
            type = AIResponseType.TABLE,
            data = buildLoanEMITable(emis),
            actions = listOf(
                AIActionItem("View EMIs", UIActionType.OPEN_EMI)
            )
        )
    }

    // =========================
    // TABLE BUILDER
    // =========================

    private fun buildLoanEMITable(
        deposit: List<LoanEmiWithMemberNamesDto>
    ): JsonElement {

        return buildJsonObject {

            put("columns", buildJsonArray {
                add(JsonPrimitive("Loan Number"))
                add(JsonPrimitive("Loan Amount"))
                add(JsonPrimitive("Deposited Date"))
                add(JsonPrimitive("EMI Month"))
                add(JsonPrimitive("EMI Year"))
                add(JsonPrimitive("Deposit Amount"))
                add(JsonPrimitive("Late Fee"))
                add(JsonPrimitive("Total Interest"))
            })

            put("rows", buildJsonArray {
                deposit.forEach {
                    add(buildJsonArray {
                        add(JsonPrimitive(it.loanNumber))
                        add(JsonPrimitive(it.loanAmount))
                        add(JsonPrimitive(it.emiDepositedDate))
                        add(JsonPrimitive(it.emiMonth))
                        add(JsonPrimitive(it.emiYear))
                        add(JsonPrimitive(it.emiDepositedAmount))
                        add(JsonPrimitive(it.lateFee))
                        add(JsonPrimitive(it.totalInterest))
                    })
                }
            })
        }
    }

    // =========================
    // GENERIC
    // =========================

    private fun generic() = AIResponse(
        reply = "Unsupported loan EMI action. Please ask something else or check with Apna Fund Support",
        type = AIResponseType.TEXT
    )
}