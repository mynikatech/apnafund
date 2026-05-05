package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class GroupsWithModeratorDto(
    val groupId: Int,
    val groupName: String,
    val moderator: Int? = null,
    val moderatorName: String?,
    val moderatorEmail: String?,
    val createdDate: String,
    val description: String? = null,
    val groupCode: String,
    val status: String = "ACTIVE"
)