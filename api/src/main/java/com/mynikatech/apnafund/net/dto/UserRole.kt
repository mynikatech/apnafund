package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserRole(
    val roleId: Int,
    val roleCode: String
)
