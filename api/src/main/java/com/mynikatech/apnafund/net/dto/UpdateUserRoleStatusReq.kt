package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
class UpdateUserRoleStatusReq (
    val userId: Int,
    val roleId: Int,
    val status: String
)