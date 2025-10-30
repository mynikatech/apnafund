package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class RoleWithPrivilegesDto (
    val roleId: Int,
    val roleCode: String,
    val privilegeCode: String
)