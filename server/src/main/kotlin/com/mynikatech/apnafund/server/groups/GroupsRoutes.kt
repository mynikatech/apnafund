package com.mynikatech.apnafund.server.groups

import com.mynikatech.apnafund.net.dto.AddMemberRequest
import com.mynikatech.apnafund.net.dto.GroupMembersDto
import com.mynikatech.apnafund.net.dto.GroupsDto
import com.mynikatech.apnafund.net.dto.SyncFirebaseUidRequest
import com.mynikatech.apnafund.server.api.respondError
import com.mynikatech.apnafund.server.api.respondOk
import com.mynikatech.apnafund.server.auth.FirebaseGroupService
import com.mynikatech.apnafund.server.chat.FirebaseChatService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

/**
 * SQL-object style (like UsersSql). If you kept a GroupsRepo façade,
 * you can adapt this easily—just change the parameter type and calls.
 */
fun Route.groupsRoutes(groups: GroupsSql) = route("/groups") {

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
        val dto = call.receive<GroupsDto>()

        if (dto.groupName.isNullOrBlank()) {
            call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "groupName required"
            )
            return@post
        }

        val id = groups.addGroup(dto)

        call.application.log.info("Creating Firebase group for groupId=$id")

        try {
            FirebaseGroupService.createGroup(
                groupId = id,
                groupName = dto.groupName
            )
        } catch (e: Exception) {
            call.application.log.error(
                "🔥 Firebase group creation failed for groupId=$id",
                e
            )

            // IMPORTANT: decide your consistency strategy (see below)
            call.respondError(
                HttpStatusCode.InternalServerError,
                "firebase_error",
                "Failed to create group in Firebase"
            )
            return@post
        }

        call.respondOk(id, HttpStatusCode.Created)
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

        val req = call.receive<SyncFirebaseUidRequest>()

        // 1️⃣ Validate membership in YOUR DB
        val isMember = groups.checkIfGroupMemberAlreadyAdded(
            userId = req.userId,
            groupId = req.groupId
        )

        if (!isMember) {
            call.respondError(
                HttpStatusCode.Forbidden,
                "forbidden",
                "User is not a member of this group"
            )
            return@post
        }

        // 2️⃣ Update Firestore
        FirebaseChatService.addMemberToGroup(
            groupId = req.groupId,
            firebaseUid = req.firebaseUid
        )

        call.respondOk(Unit)
    }

}
