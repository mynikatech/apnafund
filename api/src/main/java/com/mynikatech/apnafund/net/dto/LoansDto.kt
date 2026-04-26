package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoansDto(
    val loanId: Int? = null,
    val fundId: Int,
    val loanNumber: String,
    val borrowerId: Int,
    val issuedDate: String,
    val period: Double,
    val loanAmount: Double,
    val maturityDate: String,
    val rateOfInterest: Double,
    val status: String,
    val workflowStatus: String,
    val closedDate: String?,
    val closureType: String?,
    val closureSource: String?
)
