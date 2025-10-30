package com.mynikatech.apnafund.data.model

data class UserWithGroup(
    val userId: Int,
    val firstName: String,
    val lastName: String?,
    val emailId: String?,
    val phoneNumber: String?,
    val status: String,
    val userCode: String,
    val isPinSet: Boolean,
    val groupId: Int?,
    val groupName: String?,
    val moderator: Int?,
    val description: String?,
    val groupStatus: String?

)
