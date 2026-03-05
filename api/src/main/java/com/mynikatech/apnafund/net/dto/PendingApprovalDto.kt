package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class PendingApprovalDto(
    val approvalId: Int,
    val entityType: String,
    val entityId: Int,
    val title: String,
    val subtitle: String,
    val description: String?,
    val requesterName: String,
    val createdAt: String,
    val requesterUserId: Int,
    val requesterEmail: String
)