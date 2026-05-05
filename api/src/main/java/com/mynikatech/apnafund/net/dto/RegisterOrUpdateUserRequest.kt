package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterOrUpdateUserRequest(
    val user: UsersDto,
    val source: UserSaveSource,
    val groupId: Int = 0,
    val roleCode: String,
    val requestorId: Int
)
