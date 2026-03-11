package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginUserResponse(
    val user: UsersDto,
    val groups: List<UserGroup>,
    val firebaseToken: String
)
