package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterModeratorRequest(

    val user: UsersDto,
    val group: GroupsDto,
    val requestorId: Int
)
