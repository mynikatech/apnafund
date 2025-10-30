package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class RolesDto(
    val roleId: Int? = null,
    val roleCode: String,
    val roleDescription: String? = null,
    val status: String = "ACTIVE"
)
