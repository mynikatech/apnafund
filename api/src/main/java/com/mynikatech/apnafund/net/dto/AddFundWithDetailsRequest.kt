package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class AddFundWithDetailsRequest(
    val fund: FundsDto,
    val details: FundDetailsDto
)
