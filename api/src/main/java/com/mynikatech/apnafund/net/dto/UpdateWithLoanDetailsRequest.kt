package com.mynikatech.apnafund.net.dto
import kotlinx.serialization.Serializable

@Serializable
data class UpdateWithLoanDetailsRequest(
    val loan: LoansDto,
    val details: LoanDetailsDto
)