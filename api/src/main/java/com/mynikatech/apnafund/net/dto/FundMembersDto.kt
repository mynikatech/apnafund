package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class FundMembersDto(
    val fundMemberId: Int? = null,
    val userId: Int,
    val fundId: Int,
    val joiningDate: String,
    val role: String = "MEMBER",
    val status: String = "ACTIVE",
    val updatedAt: Long? = null,
    val updatedBy: Int? = null
)