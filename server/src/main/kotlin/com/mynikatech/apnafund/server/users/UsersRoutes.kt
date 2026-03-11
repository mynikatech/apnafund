package com.mynikatech.apnafund.server.users

import com.mynikatech.apnafund.net.dto.ChangePasswordRequest
import com.mynikatech.apnafund.net.dto.Channel
import com.mynikatech.apnafund.net.dto.EmailPayload
import com.mynikatech.apnafund.net.dto.FeedbackDto
import com.mynikatech.apnafund.net.dto.FirebaseTokenResp
import com.mynikatech.apnafund.net.dto.LoginUserResponse
import com.mynikatech.apnafund.net.dto.NotificationEvent
import com.mynikatech.apnafund.net.dto.RegisterModeratorRequest
import com.mynikatech.apnafund.net.dto.RegisterOrUpdateUserRequest
import com.mynikatech.apnafund.net.dto.SendEmailVerificationReq
import com.mynikatech.apnafund.net.dto.SendEmailVerificationResp
import com.mynikatech.apnafund.net.dto.UserFundDetailsDto
import com.mynikatech.apnafund.net.dto.UserPinHistoryDto
import com.mynikatech.apnafund.net.dto.UsersDto
import com.mynikatech.apnafund.net.dto.ValidateUserRequest
import com.mynikatech.apnafund.net.dto.VerifyEmailReq
import com.mynikatech.apnafund.server.api.respondError
import com.mynikatech.apnafund.server.api.respondOk
import com.mynikatech.apnafund.server.auth.FirebaseTokenService
import com.mynikatech.apnafund.server.common.http.clientIp
import com.mynikatech.apnafund.server.common.messaging.dispatch.EventDispatchService
import com.mynikatech.apnafund.server.common.messaging.factories.UserNotificationFactory
import com.mynikatech.apnafund.server.common.ratelimit.RateLimiters
import com.mynikatech.apnafund.server.common.ratelimit.RateLimiters.loginLimiter
import com.mynikatech.apnafund.server.userroles.UserRolesSql
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.json.Json

fun Route.usersRoutes(
    users: UsersSql, eventDispatchService: EventDispatchService,
    moderatorRegistrationService: ModeratorRegistrationService,
    userManagementService: UserManagementService,
    emailVerificationService: EmailVerificationService, userRoles: UserRolesSql
) = route("/users") {

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
        val ip = call.clientIp()

        if (!loginLimiter.allow("LOGIN_IP:$ip")) {
            return@get call.respondError(
                HttpStatusCode.TooManyRequests,
                "rate_limit",
                "Too many attempts. Please try later."
            )
        }

        val email = call.request.queryParameters["email"]
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "email required"
            )

        try {
            val user = users.getUserByEmail(email).firstOrNull()
                ?: return@get call.respondError(
                    HttpStatusCode.NotFound,
                    "not_found",
                    "User not found"
                )

            val userId = user.userId
                ?: return@get call.respondError(
                    HttpStatusCode.InternalServerError,
                    "internal",
                    "Invalid userId"
                )
            val userGroups = users.getBasicGroupsForUser(userId)

            val firebaseToken = FirebaseTokenService.generateFirebaseCustomToken(
                userId = userId,
                email = user.emailId,
                groupId = null
            )

            call.respondOk(
                LoginUserResponse(
                    user = user,
                    groups = userGroups,
                    firebaseToken = firebaseToken
                )
            )

        } catch (e: Exception) {
            call.application.log.error(
                "GET /users/get/by-email failed for email=$email",
                e
            )
            call.respondError(
                HttpStatusCode.InternalServerError,
                "internal",
                "Failed to fetch user"
            )
        }
    }

    get("get/by-phone") {
        val phone = call.request.queryParameters["phone"]
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "phone required"
            )
        try {
            val u = users.getUserByPhone(phone).firstOrNull()

            if (u != null) call.respondOk(u)
            else call.respondError(HttpStatusCode.NotFound, "not_found", "User not found")
        } catch (e: Exception) {
            // THIS is what was missing
            call.application.log.error(
                "GET /users/get/by-phone failed for phone=$phone",
                e
            )
            call.respondError(
                HttpStatusCode.InternalServerError,
                "internal",
                e.message ?: "Failed to fetch user"
            )
        }
    }

    get("get/with-group") {
        val gid = call.request.queryParameters["groupId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "groupId required"
            )
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
        val rows = users.getUserProfile(id).firstOrNull()
        if( null  == rows){
            call.respondError(HttpStatusCode.BadRequest, "validation", "No User Profile exists")
            return@get
        }
        val userRoles = userRoles.getRolesOfUser(id)
        val userGroups = users.getBasicGroupsForUser(id)
        rows.groups = userGroups
        rows.roles = userRoles
        call.respondOk(rows) // 200 with [] if empty
    }

    // Group member lookup
    get("get/group-member") {
        val userId = call.request.queryParameters["userId"]?.toIntOrNull()
        val groupId = call.request.queryParameters["groupId"]?.toIntOrNull()
        if (userId == null || groupId == null)
            return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "userId and groupId required"
            )
        val gm = users.getGroupMember(userId, groupId).firstOrNull()
        if (gm != null) call.respondOk(gm)
        else call.respondError(HttpStatusCode.NotFound, "not_found", "Group member not found")
    }

    get("get/exists/by-email") {
        val email = call.request.queryParameters["email"]
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "email required"
            )
        call.respondOk(users.doesUserExists(email))
    }

    get("check/pin") {
        val userId = call.request.queryParameters["userId"]?.toIntOrNull()
        val pin = call.request.queryParameters["pin"]
        if (userId == null || pin.isNullOrBlank())
            return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "userId & pin required"
            )
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

    get("get/groups-for-user/{userId}") {
        val userId = call.parameters["userId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "userId required"
            )
        val g = users.getGroupsForUser(userId)
        if (g == null) call.respondError(HttpStatusCode.NotFound, "not_found", "No groups found")
        else call.respondOk(g)
    }

    get("get/groups-for-moderator-user/{moderatorUserId}") {
        val userId = call.parameters["moderatorUserId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "userId required"
            )
        val g = users.getGroupsForModeratorUser(userId)
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
        call.respondOk(users.getTotalDeposit(uid, fid) ?: 0.0)
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
        try {
            val event = UserNotificationFactory.userRegistered(
                userId = id.toString(),
                email = dto.emailId,
                phone = "91${dto.phoneNumber}", // temp India logic
                userName = dto.fullName ?: "User"
            )
            call.application.log.info("sending email to :", dto.emailId)
            eventDispatchService.dispatchUser(event)
        } catch (ex: Exception) {
            call.application.log.error("Failed to publish USER_REGISTERED notification", ex)
        }
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

        try {
            val event = UserNotificationFactory.userUpdated(
                userId = dto.userId!!.toString(),
                email = dto.emailId,
                phone = "91${dto.phoneNumber}", // temp India logic
                userName = dto.fullName ?: "User"
            )
            eventDispatchService.dispatchUser(event)
        } catch (ex: Exception) {
            call.application.log.error("Failed to publish USER_UPDATED notification", ex)
        }
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

    post("update/{id}/password") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@post call.respondError(
                HttpStatusCode.BadRequest, "validation", "id required"
            )

        val user = users.getUserById(id)
            ?: return@post call.respondError(
                HttpStatusCode.NotFound, "not_found", "User not found"
            )

        val req = call.receive<ChangePasswordRequest>()

        userManagementService.changePassword(
            userId = id,
            rawPassword = req.newPassword
        )

        try {
            eventDispatchService.dispatchUser(
                NotificationEvent(
                    eventType = "PASSWORD_UPDATED",
                    userId = user.userId.toString(),
                    channels = setOf(Channel.EMAIL),
                    email = EmailPayload(
                        to = user.emailId,
                        userName = user.fullName ?: "User"
                    )
                )
            )
        } catch (ex: Exception) {
            call.application.log.error(
                "Failed to publish PASSWORD_UPDATED notification for userId=$id",
                ex
            )
        }

        call.respond(HttpStatusCode.NoContent)
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
        if (user == null) call.respondError(
            HttpStatusCode.Unauthorized,
            "auth",
            "Invalid credentials"
        )
        else call.respondOk(user)
    }

    post("/groups/register-moderator") {
        try {
            val req = call.receive<RegisterModeratorRequest>()
            val resp = moderatorRegistrationService.registerModeratorAndGroup(req)
            call.respondOk(resp)
        } catch (e: Exception) {
            call.application.log.error("register-moderator failed", e)
            call.respondError(
                HttpStatusCode.InternalServerError,
                "internal",
                "Failed to register moderator"
            )
        }
    }

    post("/register-update") {
        try {
            val ip = call.clientIp()

            if (!RateLimiters.registerLimiter.allow("REGISTER_IP:$ip")) {
                return@post call.respondError(
                    HttpStatusCode.TooManyRequests,
                    "rate_limit",
                    "Too many registration attempts. Please try later."
                )
            }
            val req = call.receive<RegisterOrUpdateUserRequest>()
            val resp = userManagementService.saveOrUpdateUser(req)
            call.respondOk(resp)
        } catch (e: Exception) {
            call.application.log.error("register-update failed", e)
            call.respondError(
                HttpStatusCode.InternalServerError,
                "internal",
                "Failed to register or update user"
            )
        }

    }

    post("/email/verify/send") {
        var req: SendEmailVerificationReq? = null
        val ip = call.clientIp()
        try {
            req = call.receive<SendEmailVerificationReq>()
            if (
                !RateLimiters.resendOtpLimiter.allow("RESEND_IP:$ip") ||
                !RateLimiters.resendOtpLimiter.allow("RESEND_USER:${req.userId}")
            ) {
                return@post call.respondError(
                    HttpStatusCode.TooManyRequests,
                    "rate_limit",
                    "Please wait before requesting another code."
                )
            }
            val expiresAtMillis =
                emailVerificationService.sendVerificationEmail(
                    userId = req.userId,
                    email = req.emailId,
                    userName = req.userName,
                    purpose = req.purpose
                )

            call.respondOk(
                SendEmailVerificationResp(
                    emailOtpExpiresAtMillis = expiresAtMillis
                )
            )
        } catch (e: Exception) {
            call.application.log.error(
                "email verify send failed for userId=${req?.userId ?: "unknown"}",
                e
            )
            call.respondError(
                HttpStatusCode.InternalServerError,
                "internal",
                "Failed to send verification email"
            )
        }
    }

    get("/is-email-verified/{userId}") {
        val userId = call.parameters["userId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "userId required"
            )

        try {
            val verified = emailVerificationService.isEmailVerified(userId)
            call.respondOk(verified)
        } catch (e: Exception) {
            call.application.log.error(
                "is-email-verified failed for userId=$userId",
                e
            )
            call.respondError(
                HttpStatusCode.InternalServerError,
                "internal",
                "Failed to check email verification status"
            )
        }
    }

    post("/email/verify/confirm") {
        try {
            val req = call.receive<VerifyEmailReq>()

            emailVerificationService.verifyEmail(
                otp = req.token,
                userId = req.userId,
                purpose = req.purpose
            )

            call.respondOk(true)

        } catch (e: BadRequestException) {
            call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                e.message ?: "Invalid or expired token"
            )

        } catch (e: Exception) {
            call.application.log.error("email verify confirm failed", e)
            call.respondError(
                HttpStatusCode.InternalServerError,
                "internal",
                "Email verification failed"
            )
        }
    }
    post("/firebase-uid/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, "id required")
            return@post
        }

        val existing = users.getUser(id)
        if (existing.isEmpty()) {
            call.respond(HttpStatusCode.NotFound)
            return@post
        }

        val req = call.receive<UsersDto>()

        if (req.firebaseUserId.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "firebaseUserId required")
            return@post
        }

        // ✅ Force correct identity
        val user = existing.first()

        val updatedDto = user.copy(
            firebaseUserId = req.firebaseUserId
        )

        // ✅ Reuse existing UPSERT
        users.upsertUserByEmail(updatedDto)

        call.respond(HttpStatusCode.NoContent)
    }

    post("/firebase-token/{userId}") {
        val userId = call.parameters["userId"]!!.toInt()

        try {
            val user = users.getUserById(userId)
                ?: return@post call.respond(HttpStatusCode.NotFound)

            val firebaseToken = FirebaseTokenService.generateFirebaseCustomToken(
                userId = userId,
                email = user.emailId,
                groupId = null
            )

            call.respondOk(
                FirebaseTokenResp(firebaseToken = firebaseToken)
            )

        } catch (e: Exception) {
            call.application.log.error(
                "POST /users/firebase-token/$userId",
                e
            )
            call.respondError(
                HttpStatusCode.InternalServerError,
                "internal",
                "Failed to generate Firebase token"
            )
        }
    }
}

