package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable


@Serializable
data class LoanDetailsDto(
    val loanDetailsId: Int? = null,
    val loanId: Int,
    val origPrincipal: Double,
    val totalInterest: Double,
    val totalAmount: Double,
    val emiInterest: Double,
    val currTotalIntPaid: Double,
    val currPrincipal: Double
)