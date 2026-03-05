package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApprovalInfoDto(
    val approvalId: Int,
    val entityType: String,
    val entityId: Int,
    val requestedBy: Int,
    val approverUserId: Int,
    val approvalStatus: String
)
