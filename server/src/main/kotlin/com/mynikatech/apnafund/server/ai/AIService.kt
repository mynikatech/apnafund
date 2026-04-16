package com.mynikatech.apnafund.server.ai

import com.github.benmanes.caffeine.cache.Caffeine
import com.mynikatech.apnafund.net.dto.AIActionItem
import com.mynikatech.apnafund.net.dto.AIContext
import com.mynikatech.apnafund.net.dto.AIEntity
import com.mynikatech.apnafund.net.dto.AIIntent
import com.mynikatech.apnafund.net.dto.AIIntentResult
import com.mynikatech.apnafund.net.dto.AIRequest
import com.mynikatech.apnafund.net.dto.AIAction
import com.mynikatech.apnafund.net.dto.AIResponse
import com.mynikatech.apnafund.net.dto.AIResponseType
import com.mynikatech.apnafund.net.dto.UIActionType
import com.mynikatech.apnafund.server.users.UserFinanceService
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.TimeUnit

class AIService(
    private val userFinanceService: UserFinanceService,
    private val aiClient: AIClient,
    private val loanHandler: LoanHandler,
    private val fundHandler: FundHandler,
    private val helpHandler: HelpHandler,
    private val groupHandler: GroupHandler,
    private val depositHandler: DepositHandler,
    private val loanEMIHandler: LoanEMIHandler
) {
    private val parser = AIIntentParser()
    private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)

    private val aiCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String, AIIntentResult>()

    suspend fun processQuery(request: AIRequest): AIResponse {

        val message = request.message.lowercase()
        val context = request.context
        val intent = getIntent(message)
        return handleIntent(intent, context)
    }

    private fun handleSummary(userId: Int): AIResponse {

        val groups = userFinanceService.getGroupssForUser(userId)
        val funds = userFinanceService.getFundsForUser(userId)
        val loans = userFinanceService.getLoansForUser(userId)

        val fundToGroupMap = funds.associateBy { it.fundId }

        val groupedData = groups.map { group ->

            val groupFunds = funds.filter { it.groupId == group.groupId }

            val groupLoans = loans.filter { loan ->
                fundToGroupMap[loan.fundId]?.groupId == group.groupId
            }

            buildJsonObject {
                put("groupName", group.groupName)
                put("fundCount", groupFunds.size)
                put("loanCount", groupLoans.size)
                put("totalFunds", groupFunds.sumOf { it.totalExpectedDeposit })
                put("totalLoans", groupLoans.sumOf { it.loanAmount })
            }
        }

        val totalFunds = funds.sumOf { it.totalExpectedDeposit }
        val totalLoans = loans.sumOf { it.loanAmount }

        val data = buildJsonObject {

            put("groups", buildJsonArray {
                groupedData.forEach { add(it) }
            })

            put("overall", buildJsonObject {
                put("totalFunds", totalFunds)
                put("totalLoans", totalLoans)
                put("fundCount", funds.size)
                put("loanCount", loans.size)
            })
        }

        return AIResponse(
            reply = "Here is your group-wise summary.",
            type = AIResponseType.SUMMARY,
            data = data
        )
    }

    private fun handleGeneric(): AIResponse {
        return AIResponse(
            reply = "I didn’t quite get that. Try asking things like:\n• Show my loans\n• Show pending loans\n•" +
                    " Show fund summary\n. Show my groups\n. help with Loans, funds etc",
            type = AIResponseType.TEXT,
            actions = listOf(
                AIActionItem(
                    label = "View Funds",
                    type = UIActionType.OPEN_FUNDS
                ),
                AIActionItem(
                    label = "View Loans",
                    type = UIActionType.OPEN_LOANS
                )
            )
        )
    }

    private fun handleIntent(
        intent: AIIntentResult,
        context: AIContext
    ): AIResponse {

        if (intent.intent == AIIntent.HELP) {
            return helpHandler.handle(intent)
        }

        return when (intent.entity) {

            AIEntity.LOAN -> loanHandler.handle(intent, context)

            AIEntity.FUND -> fundHandler.handle(intent, context)

            AIEntity.GROUP -> groupHandler.handle(intent, context)

            AIEntity.DEPOSIT -> depositHandler.handle(intent, context)

            AIEntity.LOAN_EMI -> loanEMIHandler.handle(intent, context)

            null -> {
                if (intent.intent == AIIntent.SUMMARY) {
                    handleSummary(context.userId)
                } else {
                    handleGeneric()
                }
            }

            else -> handleGeneric()
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

        val rawResult = try {
            aiClient.getIntentFromAI(message)
        } catch (e: Exception) {
            log.error("AI failed, using parser: ${e.message}")
            parser.parse(message)
        }
        val result = sanitizeIntent(rawResult, message)
        // Store result
        aiCache.put(cacheKey, result)

        return result
    }

    private fun sanitizeIntent(
        intent: AIIntentResult,
        message: String
    ): AIIntentResult {

        val supportedActions = setOf(
            AIAction.LIST,
            AIAction.DETAILS,
            AIAction.SUMMARY,
            AIAction.MEMBERS,
            AIAction.CREATE,
            AIAction.NAVIGATE
        )

        // Unsupported action → downgrade to UNKNOWN
        if (intent.action !in supportedActions) {

            log.warn("Unsupported action detected: ${intent.action} for message: $message")

            return AIIntentResult(
                intent = AIIntent.UNKNOWN,
                entity = null,
                filters = JsonObject(emptyMap()),
                action = AIAction.LIST // safe fallback
            )
        }

        return intent
    }


}