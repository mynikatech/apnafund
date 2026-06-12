package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class SendOtpReq(
    val userId: Int,
    val phoneNumber: String,
    val purpose: String
)
