package com.mynikatech.apnafund.server.approval

import com.mynikatech.apnafund.net.dto.ApprovalInfoDto
import com.mynikatech.apnafund.server.auth.FirebaseGroupService
import com.mynikatech.apnafund.server.db.Db.jdbi
import com.mynikatech.apnafund.server.funds.FundsSql
import com.mynikatech.apnafund.server.groups.GroupsSql
import com.mynikatech.apnafund.server.loans.LoansSql
import com.mynikatech.apnafund.server.notifications.NotificationService
import com.mynikatech.apnafund.server.users.UsersSql
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ApprovalService(
    private val approvalSql: ApprovalSql,
    private val loansSql: LoansSql,
    private val groupsSql: GroupsSql,
    private val fundSql: FundsSql,
    private val usersSql: UsersSql,
    private val notificationService: NotificationService
) {
    private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)

    fun approve(
        approvalId: Int,
        approvedBy: Int,
        reason: String?
    ) {

        val approval =
            approvalSql.getApprovalById(approvalId)
                ?: error("Approval not found")

        validateApprovalPermission(
            approval,
            approvedBy
        )

        when (approval.entityType) {

            "LOAN" ->
                approveLoan(approval, approvedBy, reason)

            "LOAN_CLOSURE" ->
                approveLoanClosure(approval, approvedBy, reason)   // ✅ ADD

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

        validateApprovalPermission(
            approval,
            rejectedBy
        )
        when (approval.entityType) {

            "LOAN" ->
                rejectLoan(approval, rejectedBy, reason)

            "LOAN_CLOSURE" ->
                rejectLoanClosure(approval, rejectedBy, reason)

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

        // DB TRANSACTION
        jdbi.useTransaction<Exception> { handle ->

            approvalSql.approveApproval(
                approval.approvalId,
                approvedBy,
                reason
            )

            groupsSql.activateGroup(groupId)
            groupsSql.activateGroupMemberships(groupId,approvedBy)

        }
        val group = groupsSql.getGroup(groupId).firstOrNull() ?: error("Group not found")
        // activate moderator role code.
        usersSql.upsertUserRoleByCode(
            group.moderator!!,
            "GROUP_MODERATOR",
            "ACTIVE"
        )
        // create firebase group

        try {
            FirebaseGroupService.createGroup(
                groupId = group.groupId!!,
                groupName = group.groupName,
                createdByUserId = group.moderator
            )
        } catch (e: Exception) {
            log.error(
                "Firebase group creation failed for groupId=${group.groupId}",
                e
            )
        }

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

    private fun approveLoanClosure(
        approval: ApprovalInfoDto,
        approvedBy: Int,
        reason: String?
    ) {

        val loanId = approval.entityId

        // Approve request
        approvalSql.approveApproval(
            approval.approvalId,
            approvedBy,
            reason
        )

        // Trigger actual closure (IMPORTANT)
        loansSql.closeLoan(loanId,
            approvedBy,
            closureSource = "REQUEST_APPROVED",
            closureDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))

        // Fetch details
        val loan = loansSql.getLoanById(loanId) ?: error("Loan not found")

        val fund = fundSql.getFund(loan.fundId).firstOrNull()
            ?: error("Fund not found")

        val borrowerUser = usersSql.getUserById(loan.borrowerId)
        val moderatorUser = usersSql.getUserById(approvedBy)

        // Notify all users (already implemented)
        notificationService.notifyLoanClosed(
            loanId = loanId,
            fundId = loan.fundId,
            fundName = fund.fundName,
            borrower = borrowerUser,
            closureType = loan.closureType ?: "UNKNOWN",
            closedByName = moderatorUser.fullName
        )
    }

    private fun rejectLoanClosure(
        approval: ApprovalInfoDto,
        rejectedBy: Int,
        reason: String
    ) {

        val loanId = approval.entityId

        // Reject approval
        approvalSql.rejectApproval(
            approval.approvalId,
            rejectedBy,
            reason
        )

        val loan = loansSql.getLoanById(loanId) ?: error("Loan not found")

        val fund = fundSql.getFund(loan.fundId).firstOrNull()
            ?: error("Fund not found")

        val borrowerUser = usersSql.getUserById(loan.borrowerId)
        val rejectedByUser = usersSql.getUserById(rejectedBy)

        // Notify borrower
        notificationService.notifyLoanClosureRejected(
            loanId = loanId,
            fundName = fund.fundName,
            borrower = borrowerUser,
            rejectedByName = rejectedByUser.fullName,
            reason = reason
        )
    }

    private fun validateApprovalPermission(
        approval: ApprovalInfoDto,
        userId: Int
    ) {

        when (approval.entityType) {

            "LOAN",
            "LOAN_CLOSURE" -> {

                val loan =
                    loansSql.getLoanById(
                        approval.entityId
                    ) ?: error("Loan not found")

                val moderators =
                    fundSql.getActiveFundModerators(
                        loan.fundId
                    )

                val isFundModerator =
                    moderators.any {
                        it.userId == userId
                    }

                if (!isFundModerator) {

                    error(
                        "User not authorized to approve loan workflow"
                    )
                }
            }

            "GROUP" -> {

                val isAdmin =
                    usersSql.getAdminUsers().any {
                        it.userId == userId
                    }

                if (!isAdmin) {

                    error(
                        "User not authorized to approve group workflow"
                    )
                }
            }

            else -> {

                error(
                    "Unsupported approval type"
                )
            }
        }
    }
}