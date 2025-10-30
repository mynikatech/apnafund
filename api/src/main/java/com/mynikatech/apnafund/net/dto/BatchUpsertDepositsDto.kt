package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class BatchUpsertDepositsDto(
    val fundId: Int,
    val depositMonth: String,
    val depositYear: String,
    val rows: List<DepositRowStateDto>
)