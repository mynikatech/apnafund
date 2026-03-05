package com.mynikatech.apnafund.server.common.messaging.factories

import com.mynikatech.apnafund.net.dto.SupportEvent
import com.mynikatech.apnafund.net.dto.SupportSource

object SupportEventFactory {

    fun feedback(
        userId: Long?,
        email: String?,
        message: String
    ) = SupportEvent(
        source = SupportSource.FEEDBACK,
        userId = userId,
        userEmail = email,
        subject = "User Feedback",
        message = message
    )
}