package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserGroup(
    val groupId: Int,
    val groupName: String
)
