package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoanDetailsWithMemberNamesDto(
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
    val emiInterest: Double,
    val currPrincipal: Double,
    val firstName: String,
    val lastName: String,
    val userId: Int,
    val loanDetailsId: Int? = null,
    val currTotalIntPaid: Double,
    val totalInterest: Double,
    val workflowStatus: String,
    val closedDate: String?,
    val closureType: String?,
    val closureSource: String?
) {
    val borrowerName: String
        get() = "$firstName ${lastName}".trim()
}