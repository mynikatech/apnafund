package com.mynikatech.apnafund.server.ai


import com.mynikatech.apnafund.net.dto.*
import com.mynikatech.apnafund.server.users.UserFinanceService
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

class LoanHandler(
    private val userFinanceService: UserFinanceService,
    private val responseBuilder: AIResponseBuilder
) {

    fun handle(
        intent: AIIntentResult,
        context: AIContext
    ): AIResponse {

        val loans = getFilteredLoans(context.userId, intent.filters)

        return when (intent.action) {

            AIAction.LIST -> buildLoanList(loans, intent.filters)

            AIAction.DETAILS -> buildLoanDetails(loans, intent.filters)

            AIAction.SUMMARY -> buildLoanSummary(loans)

            else -> generic()
        }
    }

    // =========================
    // 🔥 DATA LAYER
    // =========================

    private fun getFilteredLoans(
        userId: Int,
        filters: JsonObject
    ): List<LoanDetailsWithMemberNamesDto> {

        val loans = userFinanceService.getLoansForUser(userId)

        val statusFilter = filters["status"]?.jsonPrimitive?.contentOrNull
        val borrowerFilter = filters["borrower"]?.jsonPrimitive?.contentOrNull

        return loans.filter { loan ->

            val statusMatch = statusFilter?.let { input ->
                StatusMapper.map(AIEntity.LOAN, input).any {
                    loan.status.equals(it, ignoreCase = true)
                }
            } ?: true

            val borrowerMatch = borrowerFilter?.let {
                loan.borrowerName.contains(it, ignoreCase = true)
            } ?: true

            statusMatch && borrowerMatch
        }
    }

    // =========================
    // LIST
    // =========================

    private fun buildLoanList(
        loans: List<LoanDetailsWithMemberNamesDto>,
        filters: JsonObject
    ): AIResponse {

        if (loans.isEmpty()) {
            return responseBuilder.buildEmptyResponse(AIEntity.LOAN, filters)
        }

        return AIResponse(
            reply = "Here are your ${loans.size} loans:",
            type = AIResponseType.TABLE,
            data = buildLoanTable(loans),
            actions = listOf(
                AIActionItem("View Loans", UIActionType.OPEN_LOANS)
            )
        )
    }

    // =========================
    // DETAILS
    // =========================

    private fun buildLoanDetails(
        loans: List<LoanDetailsWithMemberNamesDto>,
        filters: JsonObject
    ): AIResponse {

        val loan = loans.firstOrNull()
            ?: return responseBuilder.buildEmptyResponse(AIEntity.LOAN, filters)

        val data = buildJsonObject {
            put("loanNumber", JsonPrimitive(loan.loanNumber))
            put("borrower", JsonPrimitive(loan.borrowerName))
            put("amount", JsonPrimitive(loan.loanAmount))
            put("interest", JsonPrimitive(loan.rateOfInterest))
            put("status", JsonPrimitive(loan.status))
        }

        return AIResponse(
            reply = "Here are the loan details:",
            type = AIResponseType.SUMMARY,
            data = data
        )
    }

    // =========================
    // SUMMARY
    // =========================

    private fun buildLoanSummary(
        loans: List<LoanDetailsWithMemberNamesDto>
    ): AIResponse {

        if (loans.isEmpty()) {
            return responseBuilder.buildEmptyResponse(
                AIEntity.LOAN,
                JsonObject(emptyMap())
            )
        }

        val totalAmount = loans.sumOf { it.loanAmount }

        val data = buildJsonObject {
            put("totalLoans", JsonPrimitive(loans.size))
            put("totalAmount", JsonPrimitive(totalAmount))
        }

        return AIResponse(
            reply = "You have ${loans.size} loans with total ₹$totalAmount.",
            type = AIResponseType.SUMMARY,
            data = data
        )
    }

    // =========================
    // TABLE BUILDER
    // =========================

    private fun buildLoanTable(
        loans: List<LoanDetailsWithMemberNamesDto>
    ): JsonElement {

        return buildJsonObject {

            put("columns", buildJsonArray {
                add(JsonPrimitive("Loan No"))
                add(JsonPrimitive("Borrower"))
                add(JsonPrimitive("Amount"))
                add(JsonPrimitive("Status"))
            })

            put("rows", buildJsonArray {
                loans.forEach {
                    add(buildJsonArray {
                        add(JsonPrimitive(it.loanNumber))
                        add(JsonPrimitive(it.borrowerName))
                        add(JsonPrimitive(it.loanAmount))
                        add(JsonPrimitive(it.status))
                    })
                }
            })
        }
    }

    // =========================
    // GENERIC
    // =========================

    private fun generic() = AIResponse(
        reply = "Unsupported loan action",
        type = AIResponseType.TEXT
    )
}