package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class PendingLoanEmiDto(
    val fundId: Int,
    val loanId: Int,
    val emiMonth: String,
    val emiYear: String,
    val emiDepositedDate: String?,
    val emiDepositedAmount: Double?,
    val prepaymentAmount: Double?,
    val lateFee: Double?
)
