package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class SaveOrUpdateLoanEmisRequest(
    val loanEmis: List<LoanEmisDto>,
    val fundId: Int,
    val month: String,
    val year: String
)