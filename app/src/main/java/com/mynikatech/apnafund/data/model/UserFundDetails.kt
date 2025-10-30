package com.mynikatech.apnafund.data.model

data class UserFundDetails(
    val fundDetails: FundWithDetails,
    val totalLoanAmount: Double,
    val totalDeposit: Double,
    val userExpMatAmount: Double
)
