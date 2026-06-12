package com.mynikatech.apnafund.server.users

import com.mynikatech.apnafund.net.dto.Channel
import com.mynikatech.apnafund.net.util.HashUtil
import com.mynikatech.apnafund.net.util.OtpGenerator
import io.ktor.server.plugins.BadRequestException

class OtpService(
    private val usersSql: UsersSql,
    private val whatsAppOtpService: WhatsAppOtpService,
    private val expiryMinutes: Long
) {

    fun sendOtp(
        userId: Int,
        phoneNumber: String,
        purpose: String
    ): Long {

        val otp = OtpGenerator.generate()

        val otpHash = HashUtil.sha256(otp)

        val expiresAtMillis =
            System.currentTimeMillis() + (expiryMinutes * 60_000)

        val user = usersSql.getUser(userId).firstOrNull()
            ?: throw IllegalArgumentException(
                "User not found for userId=$userId"
            )
        val userName = "${user.firstName} ${user.lastName}".trim()

        usersSql.createVerificationToken(
            userId = userId,
            channel = Channel.WHATSAPP.name,
            purpose = purpose,
            tokenHash = otpHash,
            expiresAtMillis = expiresAtMillis
        )

        whatsAppOtpService.sendOtp(
            userId = userId,
            phoneNumber = phoneNumber,
            userName = userName,
            otp = otp,
            purpose = purpose
        )

        return expiresAtMillis
    }

    fun verifyOtp(
        otp: String,
        userId: Int,
        purpose: String,
        channel: String
    ): Int {

        val tokenHash =
            HashUtil.sha256(otp)

        val verifiedUserId =
            usersSql.verifyVerificationToken(
                tokenHash = tokenHash,
                channel = channel,
                purpose = purpose,
                userId = userId
            ) ?: throw BadRequestException(
                "Invalid or expired OTP"
            )

        if (
            channel == Channel.WHATSAPP.name &&
            purpose != "RESET_PASSWORD"
        ) {

            usersSql.markUserPhoneVerified(
                verifiedUserId
            )
        }

        return verifiedUserId
    }
}