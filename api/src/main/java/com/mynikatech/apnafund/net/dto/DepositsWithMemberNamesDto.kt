package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class DepositsWithMemberNamesDto(
    val depositId: Int? = null,
    val depositorId: Int? = null,
    val depositedDate: String? = null,
    val depositAmount: Double? = null,
    val depositMonth: String? = null,
    val depositYear: String? = null,
    val lateFee: Double? = null,
    val fundId: Int? = null,
    val firstName: String,
    val lastName: String,
    val userId: Int
)