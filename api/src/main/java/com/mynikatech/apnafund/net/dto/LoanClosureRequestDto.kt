package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoanClosureRequestDto(
    val loanId: Int,
    val requestedAmount: Double? = null,
    val remarks: String? = null,

)
