package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class FundMembersDto(
    val fundMemberId: Int? = null,
    val userId: Int,
    val fundId: Int,
    val joiningDate: String
)