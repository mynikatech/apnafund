package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class AIContext(
    val userId: Int,
    val activeFundId: Int? = null,   // hint only
    val activeGroupId: Int? = null,   // hint only
    val activeLoanId: Int? = null
)
