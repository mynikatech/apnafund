package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class SaveOrUpdateUserResponse(
    val userId: Int,

    // Email verification state
    val emailVerified: Boolean = false,

    val phoneVerified: Boolean = false,

    // OTP expiry (ISO-8601 UTC), present only if verification required
    val emailOtpExpiresAtMillis : Long? = null,

    val success: Boolean = true,

    val validationCode: String? = null,

    val validationMessage: String? = null
)
