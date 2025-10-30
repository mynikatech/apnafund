package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserRolesDto(
    val userRoleId: Int? = null,
    val userId: Int,
    val roleId: Int,
    val status: String = "ACTIVE"
)
