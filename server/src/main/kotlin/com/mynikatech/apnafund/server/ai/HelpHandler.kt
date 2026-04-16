package com.mynikatech.apnafund.server.ai

import com.mynikatech.apnafund.net.dto.AIAction
import com.mynikatech.apnafund.net.dto.AIActionItem
import com.mynikatech.apnafund.net.dto.AIEntity
import com.mynikatech.apnafund.net.dto.AIIntentResult
import com.mynikatech.apnafund.net.dto.AIResponse
import com.mynikatech.apnafund.net.dto.AIResponseType
import com.mynikatech.apnafund.net.dto.UIActionType

class HelpHandler {

    fun handle(intent: AIIntentResult): AIResponse {

        return when (intent.entity) {

            AIEntity.FUND -> handleFundHelp(intent)

            AIEntity.LOAN -> handleLoanHelp(intent)

            else -> genericHelp()
        }
    }

    private fun handleFundHelp(intent: AIIntentResult): AIResponse {

        return when (intent.action) {

            AIAction.CREATE -> AIResponse(
                reply = "Only a group moderator can add Fund. To add a fund, go to the Funds section and tap  + 'Add Fund'.",
                type = AIResponseType.TEXT,
                actions = listOf(
                    AIActionItem("Open Funds", UIActionType.OPEN_FUNDS)
                )
            )

            else -> AIResponse(
                reply = "Funds allow you to pool money and track deposits and loans.",
                type = AIResponseType.TEXT
            )
        }
    }

    private fun handleLoanHelp(intent: AIIntentResult): AIResponse {

        return when (intent.action) {

            AIAction.CREATE -> AIResponse(
                reply = "To add a loan, go to the Home Tab and tap + 'Apply Loan'.",
                type = AIResponseType.TEXT,
                actions = listOf(
                    AIActionItem("Open Funds", UIActionType.OPEN_FUNDS)
                )
            )

            else -> AIResponse(
                reply = "Loans allow members to borrow from the fund and repay with interest.",
                type = AIResponseType.TEXT
            )
        }
    }

    private fun genericHelp() = AIResponse(
        reply = "You can ask things like 'Show my funds' or 'How to add a loan'.",
        type = AIResponseType.TEXT
    )
}