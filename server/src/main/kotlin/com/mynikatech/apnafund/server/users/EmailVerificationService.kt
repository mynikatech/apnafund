package com.mynikatech.apnafund.server.users

import com.mynikatech.apnafund.net.util.HashUtil
import com.mynikatech.apnafund.net.util.OtpGenerator
import com.mynikatech.apnafund.server.common.messaging.dispatch.EventDispatchService
import com.mynikatech.apnafund.server.common.messaging.factories.UserNotificationFactory
import io.ktor.server.plugins.BadRequestException
import org.slf4j.LoggerFactory

class EmailVerificationService(
    private val usersSql: UsersSql,
    private val eventDispatchService: EventDispatchService,
    private val expiryMinutes: Long
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun sendVerificationEmail(
        userId: Int,
        email: String,
        userName: String,
        purpose: String
    ): Long {
        // 1️⃣ Generate 6-digit OTP
        val otp = OtpGenerator.generate()

        logger.info("otp generated  = {}", otp)

        // 2️⃣ Hash OTP before storing
        val otpHash = HashUtil.sha256(otp)

        // 3️⃣ Expiry (1 hour for email verification)
        val expiresAtMillis =
            System.currentTimeMillis() + (expiryMinutes * 60_000)

        // 4️⃣ Persist verification token
        usersSql.createVerificationToken(
            userId = userId,
            channel = "EMAIL",
            purpose = purpose,
            tokenHash = otpHash,
            expiresAtMillis = expiresAtMillis
        )

        // 5️⃣ Send email with RAW OTP (never the hash)
        eventDispatchService.dispatchUser(
            UserNotificationFactory.emailVerification(
                email = email,
                userId = userId,
                userName = userName,
                otp = otp
            )
        )
        return expiresAtMillis
    }

    fun verifyEmail(
        otp: String,
        userId: Int,
        purpose: String
    ): Int {

        logger.info("Purpose received from UI = {}", purpose)
        logger.info("Otp received from  UI = {}", otp)

        val tokenHash = HashUtil.sha256(otp)
        logger.info("Hashed token = {}", tokenHash)

        val verifiedUserId = usersSql.verifyVerificationToken(
            tokenHash = tokenHash,
            channel = "EMAIL",
            purpose = purpose,
            userId = userId
        ) ?: throw BadRequestException("Invalid or expired token")
        logger.info(" The returned verified User Id is: $verifiedUserId")
        when (purpose) {

            "EMAIL_VERIFY",
            "INVITE_VERIFY"-> {
                // Mark email as verified
                usersSql.markUserEmailVerified(verifiedUserId)

                val user = usersSql.getUser(verifiedUserId).firstOrNull()
                    ?: run {
                        // This should never happen
                        logger.error(
                            "Email verification token valid but user not found. userId={}",
                            verifiedUserId
                        )
                        throw IllegalStateException("User not found after OTP verification")
                    }

                // ⚠️ OPTIONAL: welcome notification (see note below)
                eventDispatchService.dispatchUser(
                    UserNotificationFactory.userEmailVerifiedWelcome(
                        userId = user.userId.toString(),
                        email = user.emailId,
                        userName = "${user.firstName} ${user.lastName}"
                    )
                )
            }

            "RESET_PASSWORD" -> {
                // do nothing
            }

            else -> {
                logger.warn("Invalid verification purpose: {}", purpose)
                throw BadRequestException("Invalid verification purpose")
            }
        }

        return verifiedUserId
    }

    fun isEmailVerified(userId: Int): Boolean =
        usersSql.isEmailVerified(userId)
}
