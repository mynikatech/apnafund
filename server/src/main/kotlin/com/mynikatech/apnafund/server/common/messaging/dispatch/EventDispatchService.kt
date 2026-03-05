package com.mynikatech.apnafund.server.common.messaging.dispatch

import com.mynikatech.apnafund.net.dto.NotificationEvent
import com.mynikatech.apnafund.net.dto.SupportEvent
import com.mynikatech.apnafund.server.common.messaging.publishers.SupportMessagingPublisher
import com.mynikatech.apnafund.server.common.messaging.publishers.UserMessagingPublisher
import org.slf4j.LoggerFactory

class EventDispatchService(
    private val userPublisher: UserMessagingPublisher,
    private val supportPublisher: SupportMessagingPublisher
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
        userPublisher.publish(event)
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
}