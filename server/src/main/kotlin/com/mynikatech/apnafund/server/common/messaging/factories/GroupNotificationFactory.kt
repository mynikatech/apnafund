package com.mynikatech.apnafund.server.common.messaging.factories

import com.mynikatech.apnafund.net.dto.Channel
import com.mynikatech.apnafund.net.dto.EmailPayload
import com.mynikatech.apnafund.net.dto.FundMemberWithNameDto
import com.mynikatech.apnafund.net.dto.GroupMemberWithNameDto
import com.mynikatech.apnafund.net.dto.NotificationEvent

object GroupNotificationFactory {

    fun fundClosed(
        fundId: String,
        fundName: String,
        recipient: FundMemberWithNameDto,
        closedByName: String,
        reason: String
    ): NotificationEvent {

        return NotificationEvent(
            eventType = "FUND_CLOSED",
            userId = recipient.userId.toString(),
            channels = setOf(Channel.EMAIL),

            email = EmailPayload(
                to = recipient.emailId,
                userName = recipient.fullName ?: "User",
                data = mapOf(
                    "fundName" to fundName,
                    "closedBy" to closedByName,
                    "reason" to reason
                )
            ),

            eventData = mapOf(
                "fundId" to fundId,
                "fundName" to fundName
            )
        )
    }

    fun fundCreated(
        fundId: String,
        fundName: String,
        recipient: GroupMemberWithNameDto,
        fundMemberNames: String
    ): NotificationEvent {

        return NotificationEvent(
            eventType = "FUND_CREATED",
            userId = recipient.userId.toString(),
            channels = setOf(Channel.EMAIL),

            email = EmailPayload(
                to = recipient.emailId,
                userName = recipient.fullName,
                data = mapOf(
                    "fundName" to fundName,
                    "fundMembers" to fundMemberNames
                )
            ),

            eventData = mapOf(
                "fundId" to fundId,
                "fundName" to fundName
            )
        )
    }



}