package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class SendEmailVerificationReq(
    val userId: Int,
    val emailId: String,
    val userName: String,
    val reason: VerificationReason = VerificationReason.REGISTRATION,
    val purpose: String
)
