package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class FundMemberFinancialSummaryDto(
    val userId: Int,
    val firstName: String,
    val lastName: String,
    val totalDeposit: Double,
    val totalLoanAmount: Double,
    val expectedMatAmount: Double,
    val totalOutstandingAmount: Double,
    val totalCurrIntPaid: Double
)