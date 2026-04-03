package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class FundAvailabilityDto(
    val totalAmount: Double,
    val approvedAmount: Double,
    val pendingAmount: Double,
    val availableAmount: Double
)
