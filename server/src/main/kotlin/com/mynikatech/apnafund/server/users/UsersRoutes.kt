package com.mynikatech.apnafund.server.users

import UsersSql
import com.mynikatech.apnafund.net.dto.FeedbackDto
import com.mynikatech.apnafund.net.dto.UserFundDetailsDto
import com.mynikatech.apnafund.net.dto.UserPasswordHistoryDto
import com.mynikatech.apnafund.net.dto.UserPinHistoryDto
import com.mynikatech.apnafund.net.dto.UsersDto
import com.mynikatech.apnafund.net.dto.ValidateUserRequest
import com.mynikatech.apnafund.server.api.respondError
import com.mynikatech.apnafund.server.api.respondOk
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.json.Json

fun Route.usersRoutes(users: UsersSql) = route("/users") {

    // ---- GETs ----
    get("get/all") { call.respondOk(users.getUsers()) }

    get("get/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, "id required"); return@get
        }
        val u = users.getUser(id).firstOrNull()
        if (u != null) call.respondOk(u)
        else call.respondError(HttpStatusCode.NotFound, "not_found", "User not found")
    }

    get("get/by-email") {
        val email = call.request.queryParameters["email"]
            ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "email required")
        val u = users.getUserByEmail(email).firstOrNull()
        if (u != null) call.respondOk(u)
        else call.respondError(HttpStatusCode.NotFound, "not_found", "User not found")
    }

    get("get/by-phone") {
        val phone = call.request.queryParameters["phone"]
            ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "phone required")
        val u = users.getUserByPhone(phone).firstOrNull()
        if (u != null) call.respondOk(u)
        else call.respondError(HttpStatusCode.NotFound, "not_found", "User not found")
    }

    get("get/with-group") {
        val gid = call.request.queryParameters["groupId"]?.toIntOrNull()
            ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "groupId required")
        call.respondOk(users.getUserWithGroup(gid))
    }

    get("get/with-group/all") {
        call.respondOk(users.getAllUsersWithGroup())
    }

    get("get/profile/{userId}") {
        val raw = call.parameters["userId"]
        val id = raw?.toIntOrNull()
        if (id == null) {
            call.respondError(HttpStatusCode.BadRequest, "validation", "userId must be an integer")
            return@get
        }
        val rows = users.getUserProfile(id)
        call.respondOk(rows) // 200 with [] if empty
    }

    // Group member lookup
    get("get/group-member") {
        val userId = call.request.queryParameters["userId"]?.toIntOrNull()
        val groupId = call.request.queryParameters["groupId"]?.toIntOrNull()
        if (userId == null || groupId == null)
            return@get call.respondError(HttpStatusCode.BadRequest, "validation", "userId and groupId required")
        val gm = users.getGroupMember(userId, groupId).firstOrNull()
        if (gm != null) call.respondOk(gm)
        else call.respondError(HttpStatusCode.NotFound, "not_found", "Group member not found")
    }

    get("get/exists/by-email") {
        val email = call.request.queryParameters["email"]
            ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "email required")
        call.respondOk(users.doesUserExists(email))
    }

    get("check/pin") {
        val userId = call.request.queryParameters["userId"]?.toIntOrNull()
        val pin = call.request.queryParameters["pin"]
        if (userId == null || pin.isNullOrBlank())
            return@get call.respondError(HttpStatusCode.BadRequest, "validation", "userId & pin required")
        call.respondOk(users.checkUserPIN(userId, pin))
    }

    get("get/count-matching") {
        val email = call.request.queryParameters["email"] ?: ""
        val phone = call.request.queryParameters["phone"] ?: ""
        val exclude = call.request.queryParameters["excludeUserId"]?.toIntOrNull() ?: 0
        call.respondOk(users.countMatchingUsers(email, phone, exclude))
    }

    get("check/group-moderator") {
        val gid = call.request.queryParameters["groupId"]?.toIntOrNull()
        if (gid == null) {
            call.respond(HttpStatusCode.BadRequest, "groupId required"); return@get
        }
        call.respondOk(users.doesGroupHasModerator(gid))
    }

    // ---- Aggregates / extras used by client ----

    get("get/group-for-user/{userId}") {
        val userId = call.parameters["userId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "userId required"
            )
        val g = users.getGroupForUser(userId).firstOrNull()
        if (g == null) call.respondError(HttpStatusCode.NotFound, "not_found", "No group found")
        else call.respondOk(g)
    }

    get("get/funds-for-user/{userId}") {
        val userId = call.parameters["userId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "userId required"
            )
        call.respondOk(users.getFundsForUser(userId))
    }

    get("get/total-deposit") {
        val uid = call.request.queryParameters["userId"]?.toIntOrNull()
        val fid = call.request.queryParameters["fundId"]?.toIntOrNull()
        if (uid == null || fid == null)
            return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "userId & fundId required"
            )
        call.respondOk(users.getTotalDeposit(uid, fid)?: 0.0)
    }

    get("get/per-member-expected-maturity-amount/{fundId}") {
        val fundId = call.parameters["fundId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "fundId required"
            )
        call.respondOk(users.getPerMemberExpectedMaturityAmount(fundId) ?: 0.0)
    }

    get("get/total-loan-amount") {
        val uid = call.request.queryParameters["userId"]?.toIntOrNull()
        val fid = call.request.queryParameters["fundId"]?.toIntOrNull()
        if (uid == null || fid == null)
            return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "userId & fundId required"
            )
        call.respondOk((users.getTotalLoanAmount(uid, fid) ?: 0.0).toDouble())
    }

    get("get/user-fund-details") {
        val uid = call.request.queryParameters["userId"]?.toIntOrNull()
        val fid = call.request.queryParameters["fundId"]?.toIntOrNull()
        if (uid == null || fid == null) {
            return@get call.respondError(
                HttpStatusCode.BadRequest, "validation", "userId & fundId required"
            )
        }
        val jsonText = users.getUserFundDetails(uid, fid)
            ?: return@get call.respondError(
                HttpStatusCode.NotFound, "not_found", "No user fund details"
            )

        val dto = Json { ignoreUnknownKeys = true }.decodeFromString<UserFundDetailsDto>(jsonText)
        call.respondOk(dto)
    }

    // ---- Mutations ----
    post("add") {
        val dto = call.receive<UsersDto>()
        if (dto.emailId.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "emailId required"); return@post
        }
        val id = users.upsertUserByEmail(dto)
        call.respondOk(id, HttpStatusCode.Created)
    }

    put("update/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, "id required"); return@put
        }
        if (users.getUser(id).isEmpty()) {
            call.respond(HttpStatusCode.NotFound); return@put
        }
        val dto = call.receive<UsersDto>()
        if (dto.emailId.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "emailId required"); return@put
        }
        users.upsertUserByEmail(dto)
        call.respond(HttpStatusCode.NoContent)
    }

    delete("delete/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@delete call.respondOk(false)     // <— envelope
        val exists = users.getUser(id).isNotEmpty()
        if (!exists) return@delete call.respondOk(false)
        users.deleteUser(id)
        call.respondOk(true)
    }

    delete("delete/all") {
        users.deleteAll()
        call.respondOk(true)
    }

    put("update/{id}/password") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respondError(HttpStatusCode.BadRequest, "validation", "id required"); return@put
        }
        if (users.getUser(id).isEmpty()) {
            call.respondError(HttpStatusCode.NotFound, "not_found", "User not found"); return@put
        }
        val req = call.receive<UserPasswordHistoryDto>()
        users.updatePassword(id, req.passwordHash)
        call.respondOk(Unit, HttpStatusCode.NoContent)
    }

    put("update/{id}/pin") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respondError(HttpStatusCode.BadRequest, "validation", "id required"); return@put
        }
        if (users.getUser(id).isEmpty()) {
            call.respondError(HttpStatusCode.NotFound, "not_found", "User not found"); return@put
        }
        val req = call.receive<UserPinHistoryDto>()
        users.updatePin(id, req.pinHash)
        call.respondOk(Unit, HttpStatusCode.NoContent)
    }

    // ---- Feedback ----
    put("feedback") {
        val req = call.receive<FeedbackDto>()
        val rows = users.insertFeedback(req)
        if (rows > 0) call.respond(HttpStatusCode.NoContent)
        else call.respond(HttpStatusCode.InternalServerError, "could not insert feedback")
    }

    get("feedback/all") {
        val items = users.getAllFeedbacks()
        if (items.isEmpty()) {
            call.respond(HttpStatusCode.NoContent); return@get
        }
        call.respond(items)
    }

    get("feedback/all/usergroup") {
        val items = users.getFeedbackWithUserGroup()
        if (items.isEmpty()) {
            call.respond(HttpStatusCode.NoContent); return@get
        }
        call.respond(items)
    }

    post("validate") {
        val req = call.receive<ValidateUserRequest>()
        val user = users.validateUser(req.emailId, req.passwordHash)
        if (user == null) call.respondError(HttpStatusCode.Unauthorized, "auth", "Invalid credentials")
        else call.respondOk(user)
    }
}
