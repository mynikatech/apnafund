package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class FundClosedData(
    val fundId: String,
    val fundName: String
) : EventData

