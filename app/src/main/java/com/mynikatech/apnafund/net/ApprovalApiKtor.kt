package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.net.dto.ApproveApprovalWorkflowDto
import com.mynikatech.apnafund.net.dto.PendingApprovalDto
import com.mynikatech.apnafund.net.dto.RejectApprovalWorkflowDto
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ApprovalApiKtor(
    private val clientProvider: () -> io.ktor.client.HttpClient = { HttpClientProvider.client }
) : ApprovalApi {
    private val client get() = clientProvider()

    override suspend fun getPendingApprovals(userId: Int): List<PendingApprovalDto> {
        return client.get("/approvals/pending/$userId")
            .unwrap()
    }

    override suspend fun approveLoan(approvalId: Int, approverId: Int): Boolean {
        return client.post("/approvals/$approvalId/approve")
        {
            contentType(ContentType.Application.Json)
            setBody(
                ApproveApprovalWorkflowDto(
                    approvalId = approvalId,
                    approverId = approverId,
                    reason = "Approved"
                )
            )
        }.unwrap<Boolean>()

    }

    override suspend fun rejectLoan(
        approvalId: Int,
        rejectorId: Int,
        reason: String?
    ): Boolean {

        return client.post("/approvals/$approvalId/reject") {
            contentType(ContentType.Application.Json)
            setBody(
                RejectApprovalWorkflowDto(
                    approvalId = approvalId,
                    rejectorId = rejectorId,
                    reason = reason
                )
            )
        }.unwrap<Boolean>()
    }

    override suspend fun approveGroup(approvalId: Int, approverId: Int): Boolean {
        return client.post("/approvals/$approvalId/approve")
        {
            contentType(ContentType.Application.Json)
            setBody(
                ApproveApprovalWorkflowDto(
                    approvalId = approvalId,
                    approverId = approverId,
                    reason = "Approved"
                )
            )
        }.unwrap<Boolean>()
    }

    override suspend fun rejectGroup(
        approvalId: Int,
        rejectorId: Int,
        reason: String?
    ): Boolean {
        return client.post("/approvals/$approvalId/reject") {
            contentType(ContentType.Application.Json)
            setBody(
                RejectApprovalWorkflowDto(
                    approvalId = approvalId,
                    rejectorId = rejectorId,
                    reason = reason
                )
            )
        }.unwrap<Boolean>()
    }
}
