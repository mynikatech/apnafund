package com.mynikatech.apnafund.server.approval

import com.mynikatech.apnafund.net.dto.ApproveApprovalWorkflowDto
import com.mynikatech.apnafund.net.dto.RejectApprovalWorkflowDto
import com.mynikatech.apnafund.server.api.respondError
import com.mynikatech.apnafund.server.api.respondOk
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.application.log

fun Route.approvalRoutes(
    approvalSql: ApprovalSql,
    approvalService: ApprovalService
) = route("/approvals") {

    get("/pending/{id}") {

        try {

            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "id required"
                )

            val approvals =
                approvalSql.getPendingApprovals(id)

            call.respondOk(approvals)

        } catch (ex: Exception) {

            call.application.log.error(
                "Error fetching pending approvals for userId=${call.parameters["id"]}",
                ex
            )

            call.respondError(
                HttpStatusCode.InternalServerError,
                "approval_fetch_failed",
                "Failed to fetch pending approvals",
                ex.message?.take(200)
            )
        }
    }

    post("/{id}/approve") {

        val approvalId =
            call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "Invalid approval id"
                )
        try {


            val dto = call.receive<ApproveApprovalWorkflowDto>()
            approvalService.approve(
                approvalId = approvalId,
                approvedBy = dto.approverId,
                reason = dto.reason ?: "Approved"
            )

            call.respondOk(true)
        }catch (ex: Exception) {

            call.application.log.error(
                "ERROR approving approvalId=$approvalId",
                ex
            )

            call.respondError(
                HttpStatusCode.InternalServerError,
                "approval_failed",
                ex.message ?: "Approval failed"
            )
        }
    }

    post("/{id}/reject") {

        val approvalId =
            call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "Invalid approval id"
                )
        try {
            val dto =
                call.receive<RejectApprovalWorkflowDto>()

            approvalService.reject(
                approvalId = approvalId,
                rejectedBy = dto.rejectorId,
                reason = dto.reason ?: "Rejected"
            )
            call.respondOk(true)
        }catch (ex: Exception) {

            call.application.log.error(
                "ERROR approving approvalId=$approvalId",
                ex
            )

            call.respondError(
                HttpStatusCode.InternalServerError,
                "approval_failed",
                ex.message ?: "Approval failed"
            )
        }
    }
}
