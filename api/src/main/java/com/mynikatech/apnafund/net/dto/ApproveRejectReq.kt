package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApproveRejectReq (
    val userId: Int,
    val roleId: Int,
    val groupId: Int,
    val userEmail: String,
    val userName: String,
    val groupName: String

)