package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChangePasswordRequest(
    val newPassword: String
)
