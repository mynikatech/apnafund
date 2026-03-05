package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApproveApprovalWorkflowDto (

        val approvalId: Int,
        val approverId: Int,
        val reason: String?
)