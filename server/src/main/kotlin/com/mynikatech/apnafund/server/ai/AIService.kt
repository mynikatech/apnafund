package com.mynikatech.apnafund.server.ai

import com.github.benmanes.caffeine.cache.Caffeine
import com.mynikatech.apnafund.net.dto.AIAction
import com.mynikatech.apnafund.net.dto.AIEntity
import com.mynikatech.apnafund.net.dto.AIIntent
import com.mynikatech.apnafund.net.dto.AIIntentResult
import com.mynikatech.apnafund.net.dto.AIRequest
import com.mynikatech.apnafund.net.dto.AIResponse
import com.mynikatech.apnafund.net.dto.AIResponseType
import com.mynikatech.apnafund.net.dto.LoanDetailsWithMemberNamesDto
import com.mynikatech.apnafund.server.users.UserFinanceService
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.concurrent.TimeUnit

class AIService(
    private val userFinanceService: UserFinanceService,
    private val aiClient: AIClient
) {
    private val parser = AIIntentParser()
    private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)

    private val aiCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String, AIIntentResult>()

    suspend fun processQuery(request: AIRequest): AIResponse {

        val message = request.message.lowercase()
        val userId = request.context.userId
        val intent = getIntent(message)

        return handleIntent(intent, userId)
    }

    private fun handleLoanQuery(userId: Int, filters: JsonObject): AIResponse {

        val loans = userFinanceService.getLoansForUser(userId)
        val statusFilter = filters["status"]?.jsonPrimitive?.contentOrNull
        val borrowerFilter = filters["borrower"]?.jsonPrimitive?.contentOrNull
        val filteredLoans = loans.filter { loan ->
            val statusMatch = statusFilter?.let { statusInput ->
                val allowedStatuses = mapStatus(statusInput)
                allowedStatuses.any { mapped ->
                    loan.status.equals(mapped, ignoreCase = true)
                }
            } ?: true

            val borrowerMatch = borrowerFilter?.let {
                loan.borrowerName.contains(it, ignoreCase = true)
            } ?: true

            statusMatch && borrowerMatch
        }

        if (filteredLoans.isEmpty()) {

            val message = buildString {
                append("You have no ")
                val statusFilter  = filters["status"]?.jsonPrimitive?.contentOrNull
                val borrowerFilter = filters["borrower"]?.jsonPrimitive?.contentOrNull
                if (statusFilter != null) {
                    append(statusFilter.lowercase())
                    append(" ")
                }

                append("loans")

                if (borrowerFilter != null) {
                    append(" for $borrowerFilter")
                }

                append(".")
            }

            return AIResponse(
                reply = message,
                type = AIResponseType.TEXT,
                actions = listOf(
                    AIAction("View Loans", "OPEN_LOANS")
                )
            )
        }
        val header = buildString {
            append("Here are your ")

            statusFilter?.let {
                append(it.lowercase())
                append(" ")
            }

            append("loans")

            borrowerFilter?.let {
                append(" for $it")
            }

            append(":")
        }
        return AIResponse(
            reply = header,
            type = AIResponseType.TABLE,
            data = buildLoanTable(filteredLoans),
            actions = listOf(
                AIAction("View Details", "OPEN_LOANS")
            )
        )
    }

    private fun handleFundQuery(userId: Int): AIResponse {

        val funds = userFinanceService.getFundsForUser(userId)
        val totalFundsSize = funds.sumOf { it.totalExpectedDeposit }
        val totalAmount = funds.sumOf { it.totalCurrAmount }

        val data = buildJsonObject {
            put("fundCount", funds.size)
            put("totalExpectedAmount", totalFundsSize)
            put("totalCurrentAmount", totalAmount)
        }

        return AIResponse(
            reply = "You are part of ${funds.size} fund(s). Current corpus is ₹$totalAmount, against expected ₹$totalFundsSize.",
            type = AIResponseType.SUMMARY,
            data = data,
            actions = listOf(
                AIAction("View Funds", "OPEN_FUNDS")
            )
        )
    }

    private fun handleSummary(userId: Int): AIResponse {

        val funds = userFinanceService.getFundsForUser(userId)
        val totalFundsSize = funds.sumOf { it.totalExpectedDeposit }
        val loans = userFinanceService.getLoansForUser(userId)

        val totalLoan = loans.sumOf { it.loanAmount }

        val data = buildJsonObject {
            put("totalFunds", totalFundsSize)
            put("totalLoans", totalLoan)
            put("fundCount", funds.size)
            put("loanCount", loans.size)
        }

        return AIResponse(
            reply = "Here’s your overall financial summary.",
            type = AIResponseType.SUMMARY,
            data = data,
            actions = listOf(
                AIAction("View Funds", "OPEN_FUNDS"),
                AIAction("View Loans", "OPEN_LOANS")
            )
        )
    }

    private fun handleGeneric(message: String): AIResponse {
        return AIResponse(
            reply = "I didn’t quite get that. Try asking things like:\n• Show my loans\n• Show pending loans\n• Show fund summary.",
            type = AIResponseType.TEXT,
            actions = listOf(
                AIAction("View Funds", "OPEN_FUNDS"),
                AIAction("View Loans", "OPEN_LOANS")
            )
        )
    }

    private fun buildLoanTable(loans: List<LoanDetailsWithMemberNamesDto>): JsonElement {

        return buildJsonObject {

            // Columns
            put("columns", buildJsonArray {
                add(JsonPrimitive("Loan No"))
                add(JsonPrimitive("Borrower"))
                add(JsonPrimitive("Amount"))
                add(JsonPrimitive("Interest"))
                add(JsonPrimitive("Status"))
            })

            // Rows
            put("rows", buildJsonArray {
                for (loan in loans) {
                    add(buildJsonArray {
                        add(JsonPrimitive(loan.loanNumber))
                        add(JsonPrimitive(loan.borrowerName))
                        add(JsonPrimitive(loan.loanAmount))
                        add(JsonPrimitive(loan.rateOfInterest))
                        add(JsonPrimitive(loan.status))
                    })
                }
            })
        }
    }
    private fun handleIntent(
        intent: AIIntentResult,
        userId: Int
    ): AIResponse {

        return when (intent.entity) {

            AIEntity.LOAN ->  handleLoanQuery(userId, intent.filters)

            AIEntity.FUND -> handleFundQuery(userId)

            null -> {
                if (intent.intent == AIIntent.SUMMARY) {
                    handleSummary(userId)
                } else {
                    handleGeneric("Intent: ${intent.intent}")
                }
            }

            else -> handleGeneric("Intent: ${intent.intent}")
        }
    }

    fun mapStatus(input: String): List<String> {
        val value = input.lowercase()
        log.debug("the filter received is ${input}")
        return when (value) {

            "paid", "completed", "done", "closed" ->
                listOf("CLOSED")

            "pending", "unpaid", "due" ->
                listOf("PENDING")

            else -> listOf(input.uppercase()) // fallback
        }
    }

    suspend fun getIntent(message: String): AIIntentResult {

        val cacheKey = message
            .lowercase()
            .replace("\\s+".toRegex(), " ")
            .trim()

        aiCache.getIfPresent(cacheKey)?.let {
            log.info("Cache hit for: $message")
            return it
        }

        val result = try {
            aiClient.getIntentFromAI(message)
        } catch (e: Exception) {
            log.error("AI failed, using parser: ${e.message}")
            parser.parse(message)
        }

        // Store result
        aiCache.put(cacheKey, result)

        return result
    }
}