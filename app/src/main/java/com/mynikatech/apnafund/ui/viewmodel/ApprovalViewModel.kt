package com.mynikatech.apnafund.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.mynikatech.apnafund.ApnaFundApplication
import com.mynikatech.apnafund.net.dto.PendingApprovalDto

class ApprovalViewModel(
) : ViewModel() {

    private val repository = ApnaFundApplication.approvalRepository

    suspend fun getPendingApprovals(userId: Int): List<PendingApprovalDto> {
        return repository.getPendingApprovals(userId)
    }

    suspend fun approveLoan(approvalId: Int, approverId: Int) {
        repository.approveLoan(approvalId, approverId)
    }

    suspend fun rejectLoan(
        approvalId: Int,
        rejectorId: Int,
        reason: String? = null
    ) {
        repository.rejectLoan(approvalId, rejectorId, reason)
    }

    suspend fun approveGroup(approvalId: Int, approverId: Int) {
        repository.approveGroup(approvalId, approverId)
    }

    suspend fun rejectGroup(
        approvalId: Int,
        rejectorId: Int,
        reason: String? = null
    ) {
        repository.rejectGroup(approvalId, rejectorId, reason)
    }
}
