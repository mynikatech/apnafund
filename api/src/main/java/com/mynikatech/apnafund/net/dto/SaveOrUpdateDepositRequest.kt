package com.mynikatech.apnafund.net.dto
import kotlinx.serialization.Serializable

@Serializable
data class SaveOrUpdateDepositRequest (

    val deposits: List<DepositsDto>,
    val fundId: Int,
    val month: String,
    val year: String
)