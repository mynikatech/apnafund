package com.mynikatech.apnafund.data.model

data class LoanDetailsWithMemberNames(

    val loanId: Int?,
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
    val loanDetailsId: Int?,
    val currTotalIntPaid: Double,
    val totalInterest: Double,
    val workflowStatus: String
){
    val borrowerName: String
        get() = "$firstName ${lastName}".trim()
}
