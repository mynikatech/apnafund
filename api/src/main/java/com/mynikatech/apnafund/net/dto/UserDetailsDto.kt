package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDetailsDto(
    val firstName: String,
    val lastName: String? = null,
    val emailId: String?,
    val phoneNumber: String? = null,
    val status: String,
    val userCode: String,
    val userRoles: List<String>,
    val userNotifications: List<UserNotificationsDto>,
    val userFunds: List<FundsDto>,
    val groups: List<UserGroup> = emptyList(),
    val pendingGroups: List<UserGroup> = emptyList()
)