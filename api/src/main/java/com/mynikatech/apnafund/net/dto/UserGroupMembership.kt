package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserGroupMembership(
    val groupId: Int,
    val role: String,
    val status: String
)