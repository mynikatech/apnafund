package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserNotificationsDto(
    val userNotificationId: Int? = null,
    val notificationType: String,
    val userId: Int,
    val message: String? = null,
    val isExpiredFlag: Boolean = false,
    val publishedFlag: Boolean = false,
    val readFlag: Boolean = false,
    val status: String = "ACTIVE"
)