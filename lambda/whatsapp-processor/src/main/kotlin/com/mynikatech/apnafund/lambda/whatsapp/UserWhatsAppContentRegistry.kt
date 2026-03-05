package com.mynikatech.apnafund.lambda.whatsapp

import com.mynikatech.apnafund.net.dto.NotificationEvent

object UserWhatsAppContentRegistry {

    data class Content(
        val message: (NotificationEvent) -> String
    )

    private val registry = mapOf(
        "USER_REGISTERED" to Content {
            "Welcome to ApnaFund! Your account has been created successfully."
        },

        "USER_UPDATED" to Content {
            "Your ApnaFund account details were updated successfully."
        },

        "FUND_CLOSED" to Content {
            val fundName = it.eventData["fundName"] ?: "your fund"
            "The fund *$fundName* has been closed successfully."
        },

        "LOAN_APPROVED" to Content {
            val amount = it.eventData["amount"] ?: "your loan"
            "Good news! $amount has been approved. Check ApnaFund for details."
        }
    )

    fun get(event: NotificationEvent): Content =
        registry[event.eventType]
            ?: Content {
                "You have a new notification from ApnaFund."
            }
}
