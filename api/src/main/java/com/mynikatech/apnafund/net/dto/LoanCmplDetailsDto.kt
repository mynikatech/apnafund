package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoanCmplDetailsDto(
    val loanId: Int,
    val fundId: Int,
    val loanDetailsId: Int,
    val loanNumber: String,
    val borrowerId: Int,
    val issuedDate: String,
    val period: Double,
    val loanAmount: Double,
    val maturityDate: String,
    val rateOfInterest: Double,
    val hasVariableInterestRate: Boolean = false,
    val revisedLoanInterestRate: Double? = null,
    val interestRateRevisionAfterMonths: Int? = null,
    val status: String,
    val emiInterest: Double,
    val totalAmount: Double,
    val currPrincipal: Double,
    val origPrincipal: Double,
    val totalInterest: Double,
    val currTotalIntPaid: Double,
    val workflowStatus: String
)