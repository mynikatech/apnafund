package com.mynikatech.apnafund.data.model

data class GroupMemberWithName(
    val groupMemberId: Int,
    val userId: Int,
    val groupId: Int,
    val joiningDate: String,
    val firstName: String,
    val lastName: String,
    val emailId: String,
    val role: String,
    val status: String
)
