package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.net.dto.PendingApprovalDto

interface ApprovalApi {

    suspend fun getPendingApprovals(userId: Int): List<PendingApprovalDto>
    suspend fun approveLoan(approvalId: Int, approverId: Int): Boolean
    suspend fun rejectLoan(approvalId: Int, rejectorId: Int, reason: String?): Boolean
    suspend fun approveGroup(approvalId: Int, approverId: Int): Boolean
    suspend fun rejectGroup(approvalId: Int, rejectorId: Int, reason: String?): Boolean
    suspend fun approveLoanClosure(approvalId: Int, approverId: Int): Boolean
    suspend fun rejectLoanClosure(approvalId: Int, rejectorId: Int, reason: String?): Boolean

}