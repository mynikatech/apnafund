package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class GroupsDto(
    val groupId: Int? = null,
    val groupName: String,
    val moderator: Int? = null,
    val createdDate: String,
    val description: String? = null,
    val groupCode: String,
    val status: String = "ACTIVE"
)