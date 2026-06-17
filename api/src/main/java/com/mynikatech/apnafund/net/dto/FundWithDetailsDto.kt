package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class FundWithDetailsDto (
    val fundName: String,
    val fundStartDate: String,
    val fundMaturityDate: String,
    val fundPeriod: Double,
    val depositionFrequency: String,
    val recurringDepositAmount: Double,
    val fundStatus: String,
    val loanInterestRate: Double,
    val hasVariableInterestRate: Boolean = false,
    val revisedLoanInterestRate: Double? = null,
    val interestRateRevisionAfterMonths: Int? = null,
    val lateFeeRate: Double,
    val monthlyDepDateBy: Int,
    val groupId: Int,
    val fundId: Int,
    val moderator: Int,
    val fundCode: String,
    val moderatorFirstName: String,
    val moderatorLastName: String?,
    val fundDetailsId: Int,
    val totalExpectedDeposit: Double,
    val totalCurrentDeposit: Double,
    val totalCurrentLateFee: Double,
    val totalCurrentInterestCollected: Double,
    val totalExpectedMaturityAmount: Double,
    val totalCurrAmount: Double,
    val groupName: String,
)