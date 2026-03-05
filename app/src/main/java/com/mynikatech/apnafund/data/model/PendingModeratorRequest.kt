package com.mynikatech.apnafund.data.model

data class PendingModeratorRequest(
    val userId: Int,
    val groupId: Int,
    val moderatorName: String,
    val groupName: String,
    val emailId: String,
    val groupDescription: String?
)
