package com.mynikatech.apnafund.server.admin

import com.mynikatech.apnafund.net.dto.ApproveRejectReq
import com.mynikatech.apnafund.net.dto.UpdateGroupStatusReq
import com.mynikatech.apnafund.net.dto.UpdateUserRoleStatusReq
import com.mynikatech.apnafund.server.api.respondError
import com.mynikatech.apnafund.server.api.respondOk
import com.mynikatech.apnafund.server.common.messaging.dispatch.EventDispatchService
import com.mynikatech.apnafund.server.common.messaging.factories.UserNotificationFactory
import com.mynikatech.apnafund.server.users.UsersSql
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route



fun Route.adminRoutes(sql: AdminSql, eventDispatchService: EventDispatchService, userSql: UsersSql) = route("/admin") {

    // Approve moderator + group
    post("approve/moderator-and-group") {
        val req = call.receive<ApproveRejectReq>()
        val moderator =
            userSql.getUser(req.userId)
                .firstOrNull()
                ?: error("Moderator not found")
        sql.approveModeratorAndGroup(req.userId, req.roleId, req.groupId)
        val event = UserNotificationFactory.moderatorGroupApproved(
            moderatorUserId = req.userId.toString(),
            moderatorName = req.userName,
            moderatorEmail = moderator.emailId,
            moderatorPhone = moderator.phoneNumber,
            groupName = req.groupName
        )
        eventDispatchService.dispatchUser(event)
        call.respondOk(Unit, HttpStatusCode.NoContent)
    }

    // Reject moderator + group
    post("reject/moderator-and-group") {
        val req = call.receive<ApproveRejectReq>()
        val moderator =
            userSql.getUser(req.userId)
                .firstOrNull()
                ?: error("Moderator not found")
        sql.rejectModeratorAndGroup(req.userId, req.roleId, req.groupId)

        val event = UserNotificationFactory.moderatorGroupRejected(
            moderatorUserId = req.userId.toString(),
            moderatorName = req.userName,
            moderatorEmail = moderator.emailId,
            moderatorPhone = moderator.phoneNumber,
            groupName = req.groupName,
            reason = "" // optional field
        )

        eventDispatchService.dispatchUser(event)
        call.respondOk(Unit, HttpStatusCode.NoContent)
    }

    // Update user role status
    put("update/user-role-status") {
        val req = call.receive<UpdateUserRoleStatusReq>()
        if (req.status.isBlank()) return@put call.respondError(
            HttpStatusCode.BadRequest,
            "validation",
            "status required"
        )
        sql.updateUserRoleStatus(req.userId, req.roleId, req.status)
        call.respondOk(Unit, HttpStatusCode.NoContent)
    }

    // Update group status
    put("update/group-status/{groupId}") {
        val groupId = call.parameters["groupId"]?.toIntOrNull()
            ?: return@put call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "groupId required"
            )
        val req = call.receive<UpdateGroupStatusReq>()
        if (req.status.isBlank()) return@put call.respondError(
            HttpStatusCode.BadRequest,
            "validation",
            "status required"
        )
        sql.updateGroupStatus(groupId, req.status)
        call.respondOk(Unit, HttpStatusCode.NoContent)
    }

}
