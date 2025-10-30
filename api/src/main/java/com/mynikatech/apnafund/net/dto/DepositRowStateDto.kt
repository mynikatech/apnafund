package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class DepositRowStateDto(
    val depositorId: Int,
    val memberName: String,
    val depositAmount: String,
    val depositDate: String,
    val lateFee: String? = null,
    var isEdited: Boolean = false
)