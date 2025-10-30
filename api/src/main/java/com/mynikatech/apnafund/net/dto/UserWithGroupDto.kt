package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserWithGroupDto(
    val userId: Int,
    val firstName: String,
    val lastName: String? = null,
    val emailId: String? = null,
    val phoneNumber: String? = null,
    val status: String,
    val userCode: String,
    val isPinSet: Boolean,
    val groupId: Int? = null,
    val groupName: String? = null,
    val moderator: Int? = null,
    val description: String? = null,
    val groupStatus: String? = null
)