package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class PrivilegeDto(
    val privilegeId: Int? = null,
    val privilegeCode: String,
    val privilegeDescription: String?,
    val status: String = "ACTIVE"
)