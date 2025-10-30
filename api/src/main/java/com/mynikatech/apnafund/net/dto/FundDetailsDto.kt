package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class FundDetailsDto(
    val fundDetailsId: Int? = null,
    val fundId: Int,
    val totalExpectedDeposit: Double,
    val totalCurrentDeposit: Double,
    val totalCurrentLateFee: Double,
    val totalCurrentInterestCollected: Double,
    val totalExpectedMaturityAmount: Double,
    val totalCurrAmount: Double
)