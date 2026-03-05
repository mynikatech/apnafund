package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class ModeratorRegistrationResponse(

    val userId: Int,

    // Email verification state
    val emailVerified: Boolean = false,

    // OTP expiry (ISO-8601 UTC), present only if verification required
    val emailOtpExpiresAtMillis : Long? = null
)
