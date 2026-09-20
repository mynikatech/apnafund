package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class CloseLoanDirectRequest(
    val loanId: Int,
    val userId: Int,
    val closureDate: String
)