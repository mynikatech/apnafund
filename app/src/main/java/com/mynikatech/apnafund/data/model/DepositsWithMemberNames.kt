package com.mynikatech.apnafund.data.model

import androidx.room.Ignore

data class DepositsWithMemberNames(
    val depositId: Int?,
    val depositorId: Int?,
    val depositedDate: String?,
    val depositAmount: Double?,
    val depositMonth: String?,
    val depositYear: String?,
    val lateFee: Double?,
    val fundId: Int?,
    val firstName: String,
    val lastName: String,
    val userId: Int,
) {
    @Ignore
    var isEdited: Boolean = false

    val fullName: String
        get() = "$firstName $lastName"

    val depositAmountStr: String
        get() = depositAmount?.toString() ?: ""

    val lateFeeStr: String
        get() = lateFee?.toString() ?: ""

    val depositedDateStr: String
        get() = depositedDate ?: ""
}
