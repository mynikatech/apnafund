package com.mynikatech.apnafund.server.common.messaging.dispatch

import com.mynikatech.apnafund.net.dto.Channel
import com.mynikatech.apnafund.net.dto.NotificationEvent
import com.mynikatech.apnafund.net.dto.SupportEvent
import com.mynikatech.apnafund.server.common.messaging.publishers.SupportMessagingPublisher
import com.mynikatech.apnafund.server.common.messaging.publishers.UserMessagingPublisher
import com.mynikatech.apnafund.server.users.UsersSql
import org.slf4j.LoggerFactory

class EventDispatchService(
    private val userPublisher: UserMessagingPublisher,
    private val supportPublisher: SupportMessagingPublisher,
    private val userSql: UsersSql
) {
    private val logger = LoggerFactory.getLogger(EventDispatchService::class.java)

    /** User-facing notifications (welcome, deposits, loans, etc.) */
    fun dispatchUser(event: NotificationEvent) {

        logger.info(
            "Dispatching USER eventType={}, userId={}, channels={}",
            event.eventType,
            event.userId,
            event.channels
        )
        val user =
            userSql.getUserById(
                event.userId.toInt()
            )

        logger.info(
            "Dispatching USER isEmailVerified={}",
            user.emailVerified
        )
        var exemptEvents = mutableSetOf(
            "GROUP_INVITE",
            "EMAIL_VERIFY",
            "EMAIL_VERIFICATION",
            "PASSWORD_RESET",
            "SET_PASSWORD",
            "GROUP_APPROVED",
            "GROUP_REJECTED"
        )
        if (
            isEmailEnabled("INVITE_NOTICE")
        ) {

            exemptEvents.add(
                "INVITE_NOTICE"
            )
        }

        val canSendEmail =
            !event.email?.to.isNullOrBlank() &&
                    (
                            user.emailVerified == true || event.eventType in exemptEvents
                            )
        val filteredChannels =
            if (!canSendEmail) {

                event.channels - Channel.EMAIL

            } else {

                event.channels
            }

        // No channels left
        if (filteredChannels.isEmpty()) {

            logger.info(
                "Skipping notification dispatch. No eligible channels for userId={}",
                event.userId
            )

            return
        }

        val updatedEvent =
            event.copy(
                channels = filteredChannels
            )

        userPublisher.publish(updatedEvent)
    }

    /** Internal support emails (feedback, contact-us, issues) */
    fun dispatchSupport(event: SupportEvent) {
        logger.info(
            "Dispatching SUPPORT event source={}, subject={}",
            event.source,
            event.subject
        )
        supportPublisher.publish(event)
    }

    fun isEmailEnabled(verificationKey: String): Boolean {

        return userSql
            .getAppConfigBoolean(
                verificationKey
            )
    }
}