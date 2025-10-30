package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class RolePrivilegeDto(
    val rolePrivilegeId: Int? = null,
    val roleId: Int,
    val privilegeId: Int,
    val status: String = "ACTIVE"
)
