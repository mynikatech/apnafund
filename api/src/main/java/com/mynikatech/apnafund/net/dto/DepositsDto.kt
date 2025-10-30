package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class DepositsDto(
    val depositId: Int? = null,
    val depositorId: Int,
    val depositedDate: String,
    val depositAmount: Double,
    val depositMonth: String,
    val depositYear: String,
    val fundId: Int,
    val lateFee: Double? = null
)