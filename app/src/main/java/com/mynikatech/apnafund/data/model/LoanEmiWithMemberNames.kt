package com.mynikatech.apnafund.data.model

import androidx.room.Ignore

data class LoanEmiWithMemberNames(

    val loanEmiId: Int?,
    val loanId: Int?,
    val emiMonth: String?,
    val emiYear: String?,
    val emiDepositedDate: String?,
    val emiDepositedAmount: Double?,
    val prepaymentAmount: Double,
    val lateFee: Double,
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
    val totalInterest: Double,
    val currTotalIntPaid: Double,
    val firstName: String,
    val lastName: String,
    val userId: Int,
    val closedDate: String?,
    val closureType: String?,
    val closureSource: String?
) {
    @Ignore
    var isEdited: Boolean = false

    val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(", ")
}
