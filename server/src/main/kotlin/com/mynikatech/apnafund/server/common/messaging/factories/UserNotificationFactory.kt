package com.mynikatech.apnafund.server.common.messaging.factories

import com.mynikatech.apnafund.net.dto.Channel
import com.mynikatech.apnafund.net.dto.EmailPayload
import com.mynikatech.apnafund.net.dto.NotificationEvent
import com.mynikatech.apnafund.server.common.messaging.NotificationHelper

object UserNotificationFactory {

    fun userRegistered(
        userId: String,
        email: String?,
        phone: String?,
        userName: String
    ) = NotificationEvent(
        eventType = "USER_REGISTERED",
        userId = userId,
        channels = buildChannels(email, phone),
        email = NotificationHelper.buildEmailPayload(
            email = email,
            userName = userName
        ),
        whatsapp = NotificationHelper.buildWhatsAppPayload(
            phone = phone,
            template = "apnafund_user_registered",
            templateParams = listOf(userName)
        ),
        eventData = mapOf(
            "userName" to userName
        )
    )

    fun userUpdated(
        userId: String,
        email: String?,
        phone: String?,
        userName: String
    ) = NotificationEvent(
        eventType = "USER_UPDATED",
        userId = userId,
        channels = buildChannels(email, phone),
        email = NotificationHelper.buildEmailPayload(
            email = email,
            userName = userName
        ),
        whatsapp = NotificationHelper.buildWhatsAppPayload(
            phone = phone,
            template = "apnafund_user_updated",
            templateParams = listOf(userName)
        ),
        eventData = mapOf(
            "userName" to userName
        )
    )

    fun loanApproved(
        userId: String,
        email: String?,
        phone: String?,
        userName: String
    ) = NotificationEvent(
        eventType = "LOAN_APPROVED",
        userId = userId,
        channels = buildChannels(email, phone),
        email = NotificationHelper.buildEmailPayload(
            email = email,
            userName = userName
        ),
        whatsapp = NotificationHelper.buildWhatsAppPayload(
            phone = phone,
            template = "apnafund_loan_approved",
            templateParams = listOf(userName)
        ),
        eventData = mapOf(
            "userName" to userName
        )

    )

    fun adminGroupPendingApproval(
        moderatorName: String,
        moderatorEmail: String,
        groupName: String,
        groupId: Int
    ) = NotificationEvent(
        eventType = "ADMIN_GROUP_PENDING_APPROVAL",
        userId = "SYSTEM", // or moderatorId if you prefer
        channels = setOf(Channel.EMAIL),
        email = EmailPayload(
            to = "support@mynikatech.in",
            userName = "Admin"
        ),
        eventData = mapOf(
            "moderatorName" to moderatorName,
            "moderatorEmail" to moderatorEmail,
            "groupName" to groupName,
            "groupId" to groupId.toString()
        )
    )

    fun moderatorGroupApproved(
        moderatorUserId: String,
        moderatorName: String,
        moderatorEmail: String?,
        moderatorPhone: String?,
        groupName: String
    ) = NotificationEvent(
        eventType = "MODERATOR_GROUP_APPROVED",
        userId = moderatorUserId,
        channels = NotificationHelper.buildChannels(
            moderatorEmail,
            moderatorPhone
        ),
        email = NotificationHelper.buildEmailPayload(
            moderatorEmail,
            moderatorName
        ),
        whatsapp = NotificationHelper.buildWhatsAppPayload(
            phone = moderatorPhone,
            template = "group_approved",
            templateParams = listOf(
                moderatorName,
                groupName
            )
        ),
        eventData = mapOf(
            "moderatorName" to moderatorName,
            "groupName" to groupName
        )
    )

    fun moderatorGroupRejected(
        moderatorUserId: String,
        moderatorName: String,
        moderatorEmail: String?,
        moderatorPhone: String?,
        groupName: String,
        reason: String? = null
    ) = NotificationEvent(
        eventType = "MODERATOR_GROUP_REJECTED",
        userId = moderatorUserId,
        channels = NotificationHelper.buildChannels(
            moderatorEmail,
            moderatorPhone
        ),
        email = NotificationHelper.buildEmailPayload(
            moderatorEmail,
            moderatorName
        ),
        whatsapp = NotificationHelper.buildWhatsAppPayload(
            phone = moderatorPhone,
            template = "apnafund_group_rejected",
            templateParams = listOf(
                moderatorName,
                groupName
            )
        ),
        eventData = buildMap {
            put("moderatorName", moderatorName)
            put("groupName", groupName)
            reason?.let { put("reason", it) }
        }
    )

    fun emailVerification(
        email: String,
        userName: String,
        userId: Int,
        otp: String
    ): NotificationEvent {
        return NotificationEvent(
            eventType = "EMAIL_VERIFICATION",
            userId = userId.toString(),
            channels = setOf(Channel.EMAIL),
            email = EmailPayload(
                to = email,
                userName = userName,
                data = mapOf(
                    "otp" to otp
                )
            )
        )
    }

    fun userEmailVerifiedWelcome(
        userId: String,
        email: String,
        userName: String
    ) = NotificationEvent(
        eventType = "EMAIL_VERIFIED_WELCOME",
        userId = userId,
        channels = setOf(Channel.EMAIL),
        email = NotificationHelper.buildEmailPayload(
            email = email,
            userName = userName,
            data = mapOf(
                "userName" to userName
            )
        )
    )

    fun devRegistrationNotice(
        userId: Int,
        email: String,
        userName: String
    ) = NotificationEvent(
        eventType = "DEV_REGISTRATION_NOTICE",
        userId = userId.toString(),
        channels = setOf(Channel.EMAIL),
        email = NotificationHelper.buildEmailPayload(
            email = email,
            userName = userName,
            data = mapOf(
                "email" to email,
                "userName" to userName
            )
        )
    )

    fun inviteNotice(
        userId: Int,
        email: String?,
        userName: String,
        phone: String?,
        invitedBy: String,
        groupName: String
    ) = NotificationEvent(
        eventType = "INVITE_NOTICE",
        userId = userId.toString(),
        channels = buildChannels(email, phone),
        email = NotificationHelper.buildEmailPayload(
            email = email,
            userName = userName,
            data = mapOf(
                "email" to (email ?: ""),
                "userName" to userName,
                "invitedBy" to invitedBy,
                "groupName" to groupName
            )
        ),
        whatsapp = NotificationHelper.buildWhatsAppPayload(
            phone = phone,
            template = "apnafund_member_invite",
            templateParams = listOf(
                userName,
                groupName,
                invitedBy
            )
        ),
        eventData = mapOf(
            "userName" to userName,
            "invitedBy" to invitedBy,
            "groupName" to groupName
        )
    )

    fun passwordUpdated(
        userId: String,
        email: String?,
        phone: String?,
        userName: String
    ) = NotificationEvent(
        eventType = "PASSWORD_UPDATED",
        userId = userId,
        channels = buildChannels(email, phone),

        email = NotificationHelper.buildEmailPayload(
            email = email,
            userName = userName
        ),

        whatsapp = NotificationHelper.buildWhatsAppPayload(
            phone = phone,
            template = "apnafund_password_updated",
            templateParams = listOf(userName)
        ),

        eventData = mapOf(
            "userName" to userName
        )
    )

    private fun buildChannels(
        email: String?,
        phone: String?
    ): Set<Channel> {

        val channels = mutableSetOf<Channel>()

        if (!email.isNullOrBlank()) {
            channels.add(Channel.EMAIL)
        }

        if (!phone.isNullOrBlank()) {
            channels.add(Channel.WHATSAPP)
        }

        return channels
    }

    fun whatsappOtp(
        userId: Int,
        phone: String,
        userName: String,
        otp: String,
        purpose: String
    ) = NotificationEvent(
        eventType = "WHATSAPP_OTP",
        userId = userId.toString(),
        channels = setOf(Channel.WHATSAPP),
        whatsapp = NotificationHelper.buildWhatsAppPayload(
            phone = phone,
            template = "apnafund_otp",
            templateParams = listOf(
                otp
            )
        ),
        eventData = mapOf(
            "otp" to otp,
            "purpose" to purpose
        )
    )

}
