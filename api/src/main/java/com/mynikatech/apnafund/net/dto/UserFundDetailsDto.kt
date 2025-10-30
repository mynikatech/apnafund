package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserFundDetailsDto(
    val fundDetails: FundWithDetailsDto,
    val totalLoanAmount: Double,
    val totalDeposit: Double,
    val userExpMatAmount: Double
)