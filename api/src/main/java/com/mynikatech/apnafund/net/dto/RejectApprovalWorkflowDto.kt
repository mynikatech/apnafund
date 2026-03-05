package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class RejectApprovalWorkflowDto (

    val approvalId: Int,
    val rejectorId: Int,
    val reason: String?
)