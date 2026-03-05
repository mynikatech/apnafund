package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class SendEmailVerificationResp(
    val emailOtpExpiresAtMillis: Long
)
