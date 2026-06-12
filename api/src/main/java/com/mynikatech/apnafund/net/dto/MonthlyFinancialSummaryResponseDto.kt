package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class MonthlyFinancialSummaryResponseDto(

    val fundId: Int,

    val fundName: String,

    val month: Int,

    val year: Int,

    val totalDepositAmount: Double? = null,

    val totalLoanIssuedAmount: Double? = null,

    val totalPrepaymentAmount: Double? = null,

    val totalInterestPaidAmount: Double? = null,

    val totalFeesPaidAmount: Double? = null,

    val totalMembers: Int = 0,

    val members: List<MonthlyMemberFinancialSummaryDto>
)
