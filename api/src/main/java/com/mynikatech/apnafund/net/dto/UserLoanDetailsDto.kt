package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserLoanDetailsDto(
    val totalCurrIntPaid: Double,
    val totalOutstandingAmount: Double
)