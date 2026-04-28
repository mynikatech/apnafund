package com.mynikatech.apnafund.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.mynikatech.apnafund.ApnaFundApplication
import com.mynikatech.apnafund.net.dto.PendingApprovalDto

class ApprovalViewModel(
) : ViewModel() {

    private val repository = ApnaFundApplication.approvalRepository

    private val loanRepository = ApnaFundApplication.loanRepository

    suspend fun getPendingApprovalsWithDetails(userId: Int): List<PendingApprovalDto> {

        val approvals = repository.getPendingApprovals(userId)

        return approvals.map { approval ->

            when (approval.entityType) {

                "LOAN_CLOSURE" -> {
                    val loan = loanRepository.getLoanById(approval.entityId)

                    approval.copy(
                        loanNumber = loan?.loanNumber,
                        requestedAmount = loan?.loanAmount.toString(),
                        loamOutstandingAmount = loan?.currPrincipal.toString()
                    )
                }
                "LOAN" -> {
                    val loan = loanRepository.getLoanById(approval.entityId)

                    approval.copy(
                        loanNumber = loan?.loanNumber,
                        loanAmount = loan?.loanAmount.toString(),
                        loanPeriod  = loan?.period.toString(),
                        LoanIntRate = loan?.rateOfInterest.toString()
                   )
                }

                else -> approval
            }
        }
    }

    suspend fun approveLoan(approvalId: Int, approverId: Int) {
        repository.approveLoan(approvalId, approverId)
    }


    suspend fun rejectLoan(approvalId: Int, rejectorId: Int, reason: String? = null ) {
        repository.rejectLoan(approvalId, rejectorId, reason)
    }

    suspend fun approveLoanClosure(approvalId: Int, approverId: Int) {
        repository.approveLoanClosure(approvalId, approverId)
    }


    suspend fun rejectLoanClosure(approvalId: Int, rejectorId: Int, reason: String? = null ) {
        repository.rejectLoanClosure(approvalId, rejectorId, reason)
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
