package com.mynikatech.apnafund.server.users

import com.mynikatech.apnafund.server.common.messaging.dispatch.EventDispatchService
import com.mynikatech.apnafund.server.common.messaging.factories.UserNotificationFactory

class WhatsAppOtpService(
    private val eventDispatchService: EventDispatchService
) {

    fun sendOtp(
        userId: Int,
        phoneNumber: String,
        userName: String,
        otp: String,
        purpose: String
    ) {
        eventDispatchService.dispatchUser(
            UserNotificationFactory.whatsappOtp(
                userId = userId,
                phone = phoneNumber,
                userName = userName,
                otp = otp,
                purpose = purpose
            )
        )
    }
}