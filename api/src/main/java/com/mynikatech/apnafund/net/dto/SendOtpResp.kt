package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class SendOtpResp(
    override val otpExpiresAtMillis: Long
) : OtpExpiryResponse
