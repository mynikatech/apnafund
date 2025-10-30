package com.mynikatech.apnafund.data.model

data class DepositRowState(
    val depositorId: Int,
    val memberName: String,
    var depositAmount: String,
    var depositDate: String,
    var lateFee: String? = null,
    var isEdited: Boolean = false
) {
    val safeLateFee: String
        get() = lateFee ?: ""
}
