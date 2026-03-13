package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class GroupCreationRequest(

    val group: GroupsDto,
    val requestorId: Int
)
