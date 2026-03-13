package com.mynikatech.apnafund.server.groups

import com.mynikatech.apnafund.net.dto.AddMemberRequest
import com.mynikatech.apnafund.net.dto.FirebaseSyncRequest
import com.mynikatech.apnafund.net.dto.GroupCreationRequest
import com.mynikatech.apnafund.net.dto.GroupMembersDto
import com.mynikatech.apnafund.net.dto.GroupsDto
import com.mynikatech.apnafund.server.api.respondError
import com.mynikatech.apnafund.server.api.respondOk
import com.mynikatech.apnafund.server.approval.ApprovalSql
import com.mynikatech.apnafund.server.auth.FirebaseGroupService
import com.mynikatech.apnafund.server.chat.FirebaseChatService
import com.mynikatech.apnafund.server.common.messaging.dispatch.EventDispatchService
import com.mynikatech.apnafund.server.common.messaging.factories.UserNotificationFactory
import com.mynikatech.apnafund.server.notifications.NotificationService
import com.mynikatech.apnafund.server.users.UsersSql
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.time.LocalDate

/**
 * SQL-object style (like UsersSql). If you kept a GroupsRepo façade,
 * you can adapt this easily—just change the parameter type and calls.
 */
fun Route.groupsRoutes(groups: GroupsSql,
                       eventDispatchService: EventDispatchService,
                       usersSql: UsersSql,
                       notificationService: NotificationService,
                       approvalSql: ApprovalSql) = route("/groups") {

    // 1) GET  /groups/get/all
    get("get/all") {
        val list = groups.getAllGroups()
        call.respondOk(list) // [] if none
    }

    // 2) GET  /groups/get/{id}
    get("get/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respondError(HttpStatusCode.BadRequest, "validation", "id required"); return@get
        }
        val dto = groups.getGroup(id).firstOrNull()
        if (dto != null) call.respondOk(dto)
        else call.respondError(HttpStatusCode.NotFound, "not_found", "Group not found")
    }

    // 3) POST /groups/add
    post("add") {

        try {

            val request = call.receive<GroupCreationRequest>()
            val dto = request.group

            if (dto.groupName.isNullOrBlank()) {
                call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "groupName required"
                )
                return@post
            }

            val group = groups.addGroup(dto)
            val groupId = group.groupId ?: throw IllegalStateException("Group id missing")


            val moderatorId = dto.moderator ?: 0
            val joiningDate = LocalDate.now().toString()
            // add group member
            groups.addGroupMember(moderatorId, groupId, joiningDate)

            if (moderatorId == request.requestorId) {

                try {

                    val moderatorUser =
                        usersSql.getUserById(moderatorId)
                            ?: throw IllegalStateException("Moderator user not found")

                    val adminUser =
                        usersSql.getAdminUser()
                            ?: throw IllegalStateException("Admin user not found")

                    approvalSql.createGroupApproval(
                        groupId,
                        request.requestorId,
                        adminUser.userId!!
                    )

                    eventDispatchService.dispatchUser(
                        UserNotificationFactory.adminGroupPendingApproval(
                            moderatorName = moderatorUser.fullName,
                            moderatorEmail = moderatorUser.emailId,
                            groupName = group.groupName,
                            groupId = groupId
                        )
                    )

                    notificationService.notifyGroupRequested(
                        groupId,
                        group.groupName,
                        moderatorUser.fullName,
                        adminUser
                    )

                } catch (e: Exception) {

                    call.application.log.error(
                        "Error creating group approval for groupId=$groupId",
                        e
                    )

                    call.respondError(
                        HttpStatusCode.InternalServerError,
                        "approval_error",
                        "Failed to create approval request"
                    )
                    return@post
                }

            } else {

                try {

                    call.application.log.info(
                        "Creating Firebase group for groupId=$groupId"
                    )

                    FirebaseGroupService.createGroup(
                        groupId = groupId,
                        groupName = group.groupName
                    )

                } catch (e: Exception) {

                    call.application.log.error(
                        "🔥 Firebase group creation failed for groupId=$groupId",
                        e
                    )

                    call.respondError(
                        HttpStatusCode.InternalServerError,
                        "firebase_error",
                        "Failed to create group in Firebase"
                    )
                    return@post
                }
            }

            call.respondOk(group, HttpStatusCode.Created)

        } catch (e: Exception) {

            call.application.log.error("Error creating group", e)

            call.respondError(
                HttpStatusCode.InternalServerError,
                "group_creation_failed",
                "Unable to create group"
            )
        }
    }

    // 4) PUT  /groups/update/{id}
    put("update/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respondError(HttpStatusCode.BadRequest, "validation", "id required"); return@put
        }
        val dto = call.receive<GroupsDto>()
        val ok = groups.updateGroup(
            id = id,
            groupName = dto.groupName,
            description = dto.description,
            moderator = dto.moderator,
            groupCode = dto.groupCode,
            status = dto.status
        )
        if (!ok) {
            call.respondError(HttpStatusCode.NotFound, "not_found", "Group not found"); return@put
        }
        call.respondOk(Unit, HttpStatusCode.NoContent)
    }

    // 5) DELETE /groups/delete/{id}
    delete("delete/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respondError(HttpStatusCode.BadRequest, "validation", "id required"); return@delete
        }
        val ok = groups.deleteGroup(id)
        // Client expects Boolean body—still use respondOk for consistency
        call.respondOk(ok)
    }

    // 6) GET /groups/get/bymod?moderator=INT
    get("get/bymod") {
        val moderatorId = call.request.queryParameters["moderator"]?.toIntOrNull()
        if (moderatorId == null) {
            call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "moderator required"
            ); return@get
        }
        val g = groups.getGroupByMod(moderatorId).firstOrNull()
        if (g != null) call.respondOk(g)
        else call.respondError(
            HttpStatusCode.NotFound,
            "not_found",
            "Group not found for moderator"
        )
    }

    // --- Members ---

    // 7) GET /groups/get/members/{groupId}
    get("get/members/{groupId}") {
        val groupId = call.parameters["groupId"]?.toIntOrNull()
        if (groupId == null) {
            call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "groupId required"
            ); return@get
        }
        call.respondOk(groups.getAllMembersofGroup(groupId))
    }

    // 8) POST /groups/add/member/{groupId}
    post("add/member/{groupId}") {
        val groupId = call.parameters["groupId"]?.toIntOrNull()
        if (groupId == null) {
            call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "groupId required"
            ); return@post
        }
        val body = call.receive<AddMemberRequest>()
        val newId = groups.addGroupMember(body.userId, groupId, body.joiningDate)
        FirebaseGroupService.addMemberToGroup(
            groupId = groupId,
            userId = body.userId
        )
        call.respondOk(newId)
    }

    // 9) GET /groups/has-moderator/{groupId}
    get("has-moderator/{groupId}") {
        val groupId = call.parameters["groupId"]?.toIntOrNull()
        if (groupId == null) {
            call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "groupId required"
            ); return@get
        }
        call.respondOk(groups.hasModerator(groupId))
    }

    // 10) DELETE /groups/delete/member
    delete("delete/member") {
        val gm = call.receive<GroupMembersDto>()
        val ok = groups.deleteGroupMember(gm)
        FirebaseGroupService.removeMemberFromGroup(gm.groupId, gm.userId)
        call.respondOk(ok)
    }

    // 11) PUT /groups/update/member
    put("update/member") {
        val gm = call.receive<GroupMembersDto>()
        groups.updateGroupMember(gm) // returns void/boolean depending on your DB fn; we ignore
        call.respondOk(Unit, HttpStatusCode.NoContent)
    }

    // 12) GET /groups/get/members/with-names/{groupId}
    get("get/members/with-names/{groupId}") {
        val groupId = call.parameters["groupId"]?.toIntOrNull()
        if (groupId == null) {
            call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "groupId required"
            ); return@get
        }
        call.respondOk(groups.getAllMembersofGroupWithNames(groupId))
    }

    // 13) GET /groups/get/members/fund/with-names/{fundId}
    get("get/members/fund/with-names/{fundId}") {
        val fundId = call.parameters["fundId"]?.toIntOrNull()
        if (fundId == null) {
            call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "fundId required"
            ); return@get
        }
        call.respondOk(groups.getGroupMembersWithNamesForFund(fundId))
    }

    // 14) GET /groups/get/members/fund/{fundId}
    get("get/members/fund/{fundId}") {
        val fundId = call.parameters["fundId"]?.toIntOrNull()
        if (fundId == null) {
            call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "fundId required"
            ); return@get
        }
        call.respondOk(groups.getGroupMembersForFundOnce(fundId))
    }

    // 15) GET /groups/members/check/{groupId}?userId=...
    get("members/check/{groupId}") {
        val groupId = call.parameters["groupId"]?.toIntOrNull()
        val userId = call.request.queryParameters["userId"]?.toIntOrNull()
        if (groupId == null || userId == null) {
            call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "groupId & userId required"
            ); return@get
        }
        call.respondOk(groups.checkIfGroupMemberAlreadyAdded(userId, groupId))
    }

    // 16) GET /groups/members/count/{groupId}
    get("members/count/{groupId}") {
        val groupId = call.parameters["groupId"]?.toIntOrNull()
        if (groupId == null) {
            call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "groupId required"
            ); return@get
        }
        call.respondOk(groups.getTotMemberNumbersForFund(groupId))
    }

    // 17) POST /groups/sync/firebase-uid
    post("sync/firebase-uid") {

        val req = call.receive<FirebaseSyncRequest>()

        call.application.log.info(
            "FIREBASE SYNC: user=${req.userId} uid=${req.firebaseUid} groups=${req.groupIds}"
        )

        val syncedGroups = mutableListOf<Int>()

        for (groupId in req.groupIds) {

            // 1️⃣ Validate membership in YOUR DB
            val isMember = groups.checkIfGroupMemberAlreadyAdded(
                userId = req.userId,
                groupId = groupId
            )

            if (!isMember) {
                call.application.log.warn(
                    "FIREBASE SYNC SKIPPED: user=${req.userId} not member of group=$groupId"
                )
                continue
            }

            // 2️⃣ Update Firestore
            FirebaseChatService.addMemberToGroup(
                groupId = groupId,
                firebaseUid = req.firebaseUid
            )

            syncedGroups.add(groupId)
        }

        call.respondOk(
            mapOf(
                "syncedGroups" to syncedGroups
            )
        )
    }

}
