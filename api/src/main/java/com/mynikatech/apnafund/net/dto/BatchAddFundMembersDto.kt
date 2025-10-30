package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
class BatchAddFundMembersDto (
    val fundId: Int,
    val members: List<FundMembersDto>
)
