package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class BatchRemoveFundMembersDto (
    val fundId: Int,
    val userIds: List<Int>
)