package com.mynikatech.apnafund.server.approval

import com.mynikatech.apnafund.net.dto.ApprovalInfoDto
import com.mynikatech.apnafund.net.dto.PendingApprovalDto
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery

interface ApprovalSql {

    @SqlQuery(
        """
        SELECT create_loan_approval(
            :loanId,
            :requestedBy,
            :approver
        )
        """
    )
    fun createLoanApproval(
        @Bind("loanId") loanId: Int,
        @Bind("requestedBy") requestedBy: Int,
        @Bind("approver") approver: Int?
    ): Boolean

    @SqlQuery(
        """
    SELECT create_loan_closure_approval(
        :loanId,
        :requestedBy,
        :approver,
        :closureType,
        :requestedAmount,
        :remarks
    )
    """
    )
    fun createLoanClosureApproval(
        @Bind("loanId") loanId: Int,
        @Bind("requestedBy") requestedBy: Int,
        @Bind("approver") approver: Int?,
        @Bind("closureType") closureType: String,
        @Bind("requestedAmount") requestedAmount: Double?,
        @Bind("remarks") remarks: String?
    ): Boolean

    @SqlQuery(
        """
        SELECT create_group_approval(
            :groupId,
            :requestedBy,
            :approver
        )
        """
    )
    fun createGroupApproval(
        @Bind("groupId") groupId: Int,
        @Bind("requestedBy") requestedBy: Int,
        @Bind("approver") approver: Int?
    ): Boolean

    @SqlQuery(
        """
        SELECT *
        FROM get_pending_approvals(:userId)
        """
    )
    fun getPendingApprovals(
        @Bind("userId") userId: Int
    ): List<PendingApprovalDto>


    @SqlQuery(
        """
        SELECT approve_approval_request(
            :approvalId,
            :approvedBy,
            :reason
        )
        """
    )
    fun approveApproval(
        @Bind("approvalId") approvalId: Int,
        @Bind("approvedBy") approvedBy: Int,
        @Bind("reason") reason: String?
    ): Boolean

    @SqlQuery(
        """
        SELECT reject_approval_request(
            :approvalId,
            :rejectedBy,
            :reason
        )
        """
    )
    fun rejectApproval(
        @Bind("approvalId") approvalId: Int,
        @Bind("rejectedBy") rejectedBy: Int,
        @Bind("reason") reason: String?

    ): Boolean

    @SqlQuery("""
    SELECT * FROM get_approval_by_id(:approvalId)
    """)
    fun getApprovalById(
        @Bind("approvalId") approvalId: Int
    ): ApprovalInfoDto?

    @SqlQuery("""
    SELECT create_auto_approved_loan_closure(:loanId, :approvedBy)
    """)
    fun createAutoApprovedClosure(
        @Bind("loanId") loanId: Int,
        @Bind("approvedBy") approvedBy: Int
    ): Boolean

}