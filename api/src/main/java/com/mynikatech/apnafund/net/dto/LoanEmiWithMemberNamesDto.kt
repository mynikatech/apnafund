package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class LoanEmiWithMemberNamesDto(
    val loanEmiId: Int? = null,
    val loanId: Int? = null,
    val emiMonth: String? = null,
    val emiYear: String? = null,
    val emiDepositedDate: String? = null,
    val emiDepositedAmount: Double? = null,
    val prepaymentAmount: Double? = null,
    val lateFee: Double? = null,
    val fundId: Int,
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
    val currPrincipal: Double,
    val totalInterest: Double,
    val currTotalIntPaid: Double,
    val firstName: String,
    val lastName: String,
    val userId: Int,
    val closedDate: String?,
    val closureType: String?,
    val closureSource: String?,
    @Transient val isEdited: Boolean = false // UI-only flag, not serialized
)