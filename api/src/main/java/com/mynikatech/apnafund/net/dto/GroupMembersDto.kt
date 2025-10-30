package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class GroupMembersDto(
    val groupMemberId: Int? = null,
    val userId: Int,
    val groupId: Int,
    val joiningDate: String
)