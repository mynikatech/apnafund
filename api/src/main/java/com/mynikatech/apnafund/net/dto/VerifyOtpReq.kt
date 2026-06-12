package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class VerifyOtpReq(
    val otp: String,
    val userId: Int,
    val purpose: String,
    val channel: String
)
