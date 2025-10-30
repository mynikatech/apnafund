package com.mynikatech.apnafund.data.model

data class FundMemberWithName(
    val fundMemberId: Int,
    val userId: Int,
    val fundId: Int,
    val joiningDate: String,
    val firstName: String,
    val lastName: String
)
