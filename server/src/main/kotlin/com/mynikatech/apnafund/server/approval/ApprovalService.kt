package com.mynikatech.apnafund.server.approval

import com.mynikatech.apnafund.net.dto.ApprovalInfoDto
import com.mynikatech.apnafund.server.common.messaging.dispatch.EventDispatchService
import com.mynikatech.apnafund.server.funds.FundsSql
import com.mynikatech.apnafund.server.groups.GroupsSql
import com.mynikatech.apnafund.server.loans.LoansSql
import com.mynikatech.apnafund.server.notifications.NotificationService
import com.mynikatech.apnafund.server.users.UsersSql

class ApprovalService(
    private val approvalSql: ApprovalSql,
    private val loansSql: LoansSql,
    private val groupsSql: GroupsSql,
    private val fundSql: FundsSql,
    private val usersSql: UsersSql,
    private val notificationService: NotificationService,
    private val eventDispatchService: EventDispatchService
) {

    fun approve(
        approvalId: Int,
        approvedBy: Int,
        reason: String?
    ) {

        val approval =
            approvalSql.getApprovalById(approvalId)
                ?: error("Approval not found")

        when (approval.entityType) {

            "LOAN" ->
                approveLoan(approval, approvedBy, reason)

            "GROUP" ->
                approveGroup(approval, approvedBy, reason)

            else ->
                error("Unsupported approval type")
        }
    }

    fun reject(
        approvalId: Int,
        rejectedBy: Int,
        reason: String
    ) {

        val approval =
            approvalSql.getApprovalById(approvalId)
                ?: error("Approval not found")

        when (approval.entityType) {

            "LOAN" ->
                rejectLoan(approval, rejectedBy, reason)

            "GROUP" ->
                rejectGroup(approval, rejectedBy, reason)

            else ->
                error("Unsupported approval type")
        }
    }

    private fun approveLoan(
        approval: ApprovalInfoDto,
        approvedBy: Int,
        reason: String?
    ) {

        val loanId = approval.entityId

        approvalSql.approveApproval(
            approval.approvalId,
            approvedBy,
            reason
        )

        loansSql.approveLoanRequest(loanId, approvedBy)

        // Get fundId, fundName, borrowerId, approverbyName
        val loan = loansSql.getLoanById(loanId) ?: error("Loan not found")
        val fundId = loan.fundId
        val fund = fundSql.getFund(fundId).firstOrNull()
            ?: error("Loan not found")
        val borrowerId = loan.borrowerId
        val approvedByUser = usersSql.getUserById(approvedBy)
        val borrowerUser = usersSql.getUserById(borrowerId)

        notificationService.notifyLoanApproved(
            loanId,
            fundId,
            fund.fundName,
            borrowerUser,
            approvedByUser.fullName

        )
    }

    private fun rejectLoan(
        approval: ApprovalInfoDto,
        rejectedBy: Int,
        reason: String
    ) {

        val loanId = approval.entityId

        approvalSql.rejectApproval(
            approval.approvalId,
            rejectedBy,
            reason
        )

        loansSql.rejectLoanRequest(loanId, rejectedBy, reason)

        // Get fundId, fundName, borrowerId, approverbyName
        val loan = loansSql.getLoanById(loanId) ?: error("Loan not found")
        val fundId = loan.fundId
        val fund = fundSql.getFund(fundId).firstOrNull()
            ?: error("Loan not found")
        val borrowerId = loan.borrowerId
        val rejectedByUser = usersSql.getUserById(rejectedBy)
        val borrowerUser = usersSql.getUserById(borrowerId)

        notificationService.notifyLoanRejected(
            loanId,
            fund.fundName,
            borrowerUser,
            rejectedByUser.fullName,
            reason
        )
    }

    private fun approveGroup(
        approval: ApprovalInfoDto,
        approvedBy: Int,
        reason: String?
    ) {

        val groupId = approval.entityId

        approvalSql.approveApproval(
            approval.approvalId,
            approvedBy,
            reason
        )

        groupsSql.activateGroup(groupId)

        notificationService.notifyGroupApproved(groupId)
    }

    private fun rejectGroup(
        approval: ApprovalInfoDto,
        rejectedBy: Int,
        reason: String
    ) {

        val groupId = approval.entityId

        approvalSql.rejectApproval(
            approval.approvalId,
            rejectedBy,
            reason
        )

        groupsSql.rejectGroup(groupId)

        notificationService.notifyGroupRejected(
            groupId,
            reason
        )
    }
}