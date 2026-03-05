package com.mynikatech.apnafund.data.repository

import com.mynikatech.apnafund.net.ApprovalApi

class ApprovalRepository(
    private val api: ApprovalApi
) {

    suspend fun getPendingApprovals(userId: Int) =
        api.getPendingApprovals(userId)

    suspend fun approveLoan(id: Int, approverId: Int) =
        api.approveLoan(id,approverId )

    suspend fun rejectLoan(id: Int, rejectorId: Int, reason: String?) =
        api.rejectLoan(id,rejectorId, reason)

    suspend fun approveGroup(id: Int, approverId: Int) =
        api.approveGroup(id, approverId)

    suspend fun rejectGroup(id: Int, rejectorId: Int, reason: String?) =
        api.rejectGroup(id, rejectorId, reason)
}