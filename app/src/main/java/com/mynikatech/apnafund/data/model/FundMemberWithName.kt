package com.mynikatech.apnafund.data.model

data class FundMemberWithName(
    val fundMemberId: Int,
    val userId: Int,
    val fundId: Int,
    val joiningDate: String,
    val firstName: String,
    val lastName: String,
    val emailId: String,
    val role: String = "MEMBER",
    val status: String = "ACTIVE",
    val updatedAt: Long? = null,
    val updatedBy: Int? = null
)
