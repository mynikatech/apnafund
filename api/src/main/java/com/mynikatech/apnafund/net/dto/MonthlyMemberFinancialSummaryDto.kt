package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class MonthlyMemberFinancialSummaryDto(

    val userId: Int,

    val firstName: String,

    val lastName: String? = null,

    val depositAmount: Double? = null,

    val latestDepositDate: String? = null,

    val loanIssuedAmount: Double? = null,

    val loanPrepaymentAmount: Double? = null,

    val interestPaidAmount: Double? = null,

    val latestEmiPaymentDate: String? = null,

    val feesPaidAmount: Double? = null
)
