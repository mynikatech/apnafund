package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserStatusResponse(
    val exists: Boolean,
    val isInvited: Boolean = false,
    val hasPasswordSet: Boolean = false
)
