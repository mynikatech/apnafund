package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class VerifyEmailReq(
    val token: String,
    val userId: Int,
    val purpose: String  // EMAIL_VERIFY, RESET_PASSWORD
)
