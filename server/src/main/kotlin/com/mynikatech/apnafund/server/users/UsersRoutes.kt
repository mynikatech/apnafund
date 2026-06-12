package com.mynikatech.apnafund.server.users

import com.mynikatech.apnafund.net.dto.AppUsageLogDto
import com.mynikatech.apnafund.net.dto.ChangePasswordRequest
import com.mynikatech.apnafund.net.dto.FeedbackDto
import com.mynikatech.apnafund.net.dto.FirebaseTokenResp
import com.mynikatech.apnafund.net.dto.LoginUserResponse
import com.mynikatech.apnafund.net.dto.RegisterModeratorRequest
import com.mynikatech.apnafund.net.dto.RegisterOrUpdateUserRequest
import com.mynikatech.apnafund.net.dto.SendEmailVerificationReq
import com.mynikatech.apnafund.net.dto.SendEmailVerificationResp
import com.mynikatech.apnafund.net.dto.SendOtpReq
import com.mynikatech.apnafund.net.dto.SendOtpResp
import com.mynikatech.apnafund.net.dto.UserFundDetailsDto
import com.mynikatech.apnafund.net.dto.UserPinHistoryDto
import com.mynikatech.apnafund.net.dto.UserStatusResponse
import com.mynikatech.apnafund.net.dto.UsersDto
import com.mynikatech.apnafund.net.dto.ValidateUserRequest
import com.mynikatech.apnafund.net.dto.VerifyEmailReq
import com.mynikatech.apnafund.net.dto.VerifyOtpReq
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
import io.ktor.server.application.ApplicationCall
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
    emailVerificationService: EmailVerificationService, userRoles: UserRolesSql, otpService: OtpService
) = route("/users") {

    // ---- GETs ----
    get("get/all") {
        call.safeRoute(
            logMessage = "GET /users/get/all failed",
            clientMessage = "Failed to fetch all users"
        ) {
            users.getUsers()
        }
    }

    get("get/for-moderator/{userId}") {
        val userId = call.parameters["userId"]?.toIntOrNull()
        if (null == userId) {
            call.respond(HttpStatusCode.BadRequest, "userId required")
            return@get
        }
        call.safeRoute(
            logMessage = "GET /users/get/for-moderator/{userId} failed",
            clientMessage = "Failed to fetch users for moderator"
        ) {


            users.getUsersForModerator(userId)
        }
    }

    get("get/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, "id required"); return@get
        }
        call.safeRoute(
            logMessage = "GET /users/get/id failed for user: $id",
            clientMessage = "Failed to fetch user"
        ) {

            val u = users.getUser(id).firstOrNull()
                ?: throw NoSuchElementException("User not found")
            u
        }
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

        call.safeRoute(
            logMessage = "GET /users/get/by-email failed for email=$email",
            clientMessage = "Failed to fetch user"
        ) {
            val user = users.getUserByEmail(email).firstOrNull()

            if (user == null) {
                return@get call.respondOk(
                    LoginUserResponse(
                        user = null,
                        groups = emptyList(),
                        pendingGroups = emptyList(),
                        firebaseToken = ""
                    )
                )
            }

            val userId = user.userId
                ?: return@get call.respondError(
                    HttpStatusCode.InternalServerError,
                    "internal",
                    "Invalid userId"
                )
            val userGroups = users.getBasicGroupsForUser(userId)
            val pendingGroups = users.getBasicPendingGroupsForUser(userId)

            val firebaseToken = FirebaseTokenService.generateFirebaseCustomToken(
                userId = userId,
                email = user.emailId,
                groupId = null
            )

            call.respondOk(
                LoginUserResponse(
                    user = user,
                    groups = userGroups,
                    pendingGroups = pendingGroups,
                    firebaseToken = firebaseToken
                )
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
        call.safeRoute(
            logMessage = "GET /users/get/by-phone failed for phone=$phone",
            clientMessage = "Failed to fetch user"
        ) {
            val u = users.getUserByPhone(phone).firstOrNull()

            if (u == null) {
                return@get call.respondOk(
                    LoginUserResponse(
                        user = null,
                        groups = emptyList(),
                        pendingGroups = emptyList(),
                        firebaseToken = ""
                    )
                )
            }
            val userId = u.userId
                ?: return@get call.respondError(
                    HttpStatusCode.InternalServerError,
                    "internal",
                    "Invalid userId"
                )
            val userGroups = users.getBasicGroupsForUser(userId)
            val pendingGroups = users.getBasicPendingGroupsForUser(userId)

            val firebaseToken = FirebaseTokenService.generateFirebaseCustomToken(
                userId = userId,
                email = u.emailId,
                groupId = null
            )

            call.respondOk(
                LoginUserResponse(
                    user = u,
                    groups = userGroups,
                    pendingGroups = pendingGroups,
                    firebaseToken = firebaseToken
                )
            )
        }
    }

    get("get/by-phone-email") {
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
        val phone = call.request.queryParameters["phone"]
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "phone required"
            )

        call.application.log.info(" the email id received is: $email")
        call.application.log.info(" the phone number received is: $phone")

        call.safeRoute(
            logMessage = "GET /users/get/by-phone-email failed for email =$email and phone =$phone",
            clientMessage = "Failed to fetch user"
        ) {
            val user = users.getUserByPhoneAndEmail(email, phone).firstOrNull()

            if (user == null) {
                call.application.log.info("no users returned")
                return@get call.respondOk(
                    LoginUserResponse(
                        user = null,
                        groups = emptyList(),
                        pendingGroups = emptyList(),
                        firebaseToken = ""
                    )
                )
            }

            val userId = user.userId
                ?: return@get call.respondError(
                    HttpStatusCode.InternalServerError,
                    "internal",
                    "Invalid userId"
                )
            val userGroups = users.getBasicGroupsForUser(userId)
            val pendingGroups = users.getBasicPendingGroupsForUser(userId)

            val firebaseToken = FirebaseTokenService.generateFirebaseCustomToken(
                userId = userId,
                email = user.emailId,
                groupId = null
            )

            call.respondOk(
                LoginUserResponse(
                    user = user,
                    groups = userGroups,
                    pendingGroups = pendingGroups,
                    firebaseToken = firebaseToken
                )
            )

        }
    }

    get("get/by-phone-group-code") {

        val phone = call.request.queryParameters["phone"]
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "phone required"
            )

        val groupCode = call.request.queryParameters["groupCode"]
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "groupCode required"
            )

        call.safeRoute(
            logMessage =
                "GET /users/get/by-phone-group-code failed for phone=$phone groupCode=$groupCode",
            clientMessage = "Failed to fetch user"
        ) {

            val user = users.getUserByPhoneAndGroupCode(
                phone,
                groupCode
            ).firstOrNull()

            if (user == null) {
                return@get call.respondOk(
                    LoginUserResponse(
                        user = null,
                        groups = emptyList(),
                        pendingGroups = emptyList(),
                        firebaseToken = ""
                    )
                )
            }

            val userId = user.userId
                ?: return@get call.respondError(
                    HttpStatusCode.InternalServerError,
                    "internal",
                    "Invalid userId"
                )

            val userGroups = users.getBasicGroupsForUser(userId)
            val pendingGroups = users.getBasicPendingGroupsForUser(userId)

            val firebaseToken =
                FirebaseTokenService.generateFirebaseCustomToken(
                    userId = userId,
                    email = user.emailId,
                    groupId = null
                )

            call.respondOk(
                LoginUserResponse(
                    user = user,
                    groups = userGroups,
                    pendingGroups = pendingGroups,
                    firebaseToken = firebaseToken
                )
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
        call.safeRoute(
            logMessage = "GET /users/get/with-group failed for groupId: =$gid",
            clientMessage = "Failed to fetch user"
        ) {
            users.getUserWithGroup(gid)
        }
    }

    get("get/with-group/all") {
        call.safeRoute(
            logMessage = "GET /users/get/with-group/all",
            clientMessage = "Failed to fetch user"
        ) {
            users.getAllUsersWithGroup()
        }
    }

    get("get/profile/{userId}") {
        val raw = call.parameters["userId"]
        val id = raw?.toIntOrNull()
        if (id == null) {
            call.respondError(HttpStatusCode.BadRequest, "validation", "userId must be an integer")
            return@get
        }
        call.safeRoute(
            logMessage = "GET /users/get/profile/$id failed",
            clientMessage = "Failed to fetch user profile"
        ) {
            val rows = users.getUserProfile(id).firstOrNull()
            if (null == rows) {
                call.respondError(HttpStatusCode.BadRequest, "validation", "No User Profile exists")
                return@get
            }
            val userRoles = userRoles.getRolesOfUser(id)
            val userGroups = users.getBasicGroupsForUser(id)
            val pendingGroups = users.getBasicPendingGroupsForUser(id)
            val groupMemberships = users.getUserGroupMemberships(id)
            val fundMemberships = users.getUserFundMemberships(id)
            rows.groups = userGroups
            rows.roles = userRoles
            rows.pendingGroups = pendingGroups
            rows.groupMemberships = groupMemberships
            rows.fundMemberships = fundMemberships
            rows
        }
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
        call.safeRoute(
            logMessage = "GET /users/get/group-member failed for userId=$userId groupId=$groupId",
            clientMessage = "Failed to fetch group member"
        ) {

            users.getGroupMember(userId, groupId).firstOrNull()
                ?: throw NoSuchElementException("Group member not found")
        }
    }
    get("/status") {
        val email = call.request.queryParameters["email"]

        if (email.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Email required")
            return@get
        }

        val user = users.getUserByEmail(email).firstOrNull()

        if (user == null) {
            call.respond(
                UserStatusResponse(
                    exists = false
                )
            )
        } else {
            call.respond(
                UserStatusResponse(
                    exists = true,
                    isInvited = user.isInvited,
                    hasPasswordSet = user.passwordHash != null
                )
            )
        }
    }

    get("get/exists/by-email") {
        val email = call.request.queryParameters["email"]
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "email required"
            )
        call.safeRoute(
            logMessage = "GET /users/get/exists/by-email failed for email=$email",
            clientMessage = "Failed to check user existence"
        ) {
            users.doesUserExists(email)
        }
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
        call.safeRoute(
            logMessage = "GET /users/check/pin failed for userId=$userId",
            clientMessage = "Failed to verify PIN"
        ) {
            users.checkUserPIN(userId, pin)
        }
    }

    get("get/count-matching") {
        val email = call.request.queryParameters["email"] ?: ""
        val phone = call.request.queryParameters["phone"] ?: ""
        val exclude = call.request.queryParameters["excludeUserId"]?.toIntOrNull() ?: 0
        call.safeRoute(
            logMessage = "GET /users/get/count-matching failed",
            clientMessage = "Failed to count users"
        ) {
            users.countMatchingUsers(email, phone, exclude)
        }
    }

    get("check/group-moderator") {
        val gid = call.request.queryParameters["groupId"]?.toIntOrNull()
        if (gid == null) {
            call.respond(HttpStatusCode.BadRequest, "groupId required"); return@get
        }
        call.safeRoute(
            logMessage = "GET /users/check/group-moderator failed for groupId=$gid",
            clientMessage = "Failed to check group moderator"
        ) {
            users.doesGroupHasModerator(gid)
        }
    }

    // ---- Aggregates / extras used by client ----

    get("get/group-for-user/{userId}") {
        val userId = call.parameters["userId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "userId required"
            )
        call.safeRoute(
            logMessage = "GET /users/get/group-for-user/$userId failed",
            clientMessage = "Failed to fetch group"
        ) {

            users.getGroupForUser(userId).firstOrNull()
                ?: throw NoSuchElementException("No group found")
        }
    }

    get("get/groups-for-user/{userId}") {
        val userId = call.parameters["userId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "userId required"
            )
        call.safeRoute(
            logMessage = "GET /users/get/groups-for-user/$userId failed",
            clientMessage = "Failed to fetch groups"
        ) {

            users.getGroupsForUser(userId)
                ?: throw NoSuchElementException("No groups found")
        }
    }

    get("get/groups-for-moderator-user/{moderatorUserId}") {
        val userId = call.parameters["moderatorUserId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "userId required"
            )
        call.safeRoute(
            logMessage = "GET /users/get/groups-for-moderator-user/$userId failed",
            clientMessage = "Failed to fetch moderator groups"
        ) {

            users.getGroupsForModeratorUser(userId)
                ?: throw NoSuchElementException("No group found")
        }
    }

    get("get/groups-for-moderator-user/moderator-info/{moderatorUserId}") {
        val userId = call.parameters["moderatorUserId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "userId required"
            )
        call.safeRoute(
            logMessage = "GET /users/get/groups-for-moderator-user/$userId failed",
            clientMessage = "Failed to fetch moderator groups"
        ) {

            users.getGroupsForModeratorUserWithModeratorInfo(userId)
                ?: throw NoSuchElementException("No group found")
        }
    }

    get("get/funds-for-user/{userId}") {
        val userId = call.parameters["userId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "userId required"
            )
        call.safeRoute(
            logMessage = "GET /users/get/funds-for-user/$userId failed",
            clientMessage = "Failed to fetch funds"
        ) {
            users.getFundsForUser(userId)
        }
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
        call.safeRoute(
            logMessage = "GET /users/get/total-deposit failed for userId=$uid fundId=$fid",
            clientMessage = "Failed to fetch total deposit"
        ) {
            users.getTotalDeposit(uid, fid) ?: 0.0
        }
    }

    get("get/per-member-expected-maturity-amount/{fundId}") {
        val fundId = call.parameters["fundId"]?.toIntOrNull()
            ?: return@get call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                "fundId required"
            )
        call.safeRoute(
            logMessage = "GET /users/get/per-member-expected-maturity-amount/$fundId failed",
            clientMessage = "Failed to fetch maturity amount"
        ) {
            users.getPerMemberExpectedMaturityAmount(fundId) ?: 0.0
        }
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
        call.safeRoute(
            logMessage = "GET /users/get/total-loan-amount failed for userId=$uid fundId=$fid",
            clientMessage = "Failed to fetch total loan"
        ) {
            users.getTotalLoanAmount(uid, fid) ?: 0.0
        }
    }

    get("get/user-fund-details") {
        val uid = call.request.queryParameters["userId"]?.toIntOrNull()
        val fid = call.request.queryParameters["fundId"]?.toIntOrNull()
        if (uid == null || fid == null) {
            return@get call.respondError(
                HttpStatusCode.BadRequest, "validation", "userId & fundId required"
            )
        }
        call.safeRoute(
            logMessage = "GET /users/get/user-fund-details failed for userId=$uid fundId=$fid",
            clientMessage = "Failed to fetch user fund details"
        ) {

            val jsonText = users.getUserFundDetails(uid, fid)
                ?: throw NoSuchElementException("No user fund details")

            Json { ignoreUnknownKeys = true }
                .decodeFromString<UserFundDetailsDto>(jsonText)
        }
    }

    // ---- Mutations ----
    post("add") {
        val dto = call.receive<UsersDto>()
        if (dto.phoneNumber.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "phone Number required"); return@post
        }
        val id = users.upsertUserByEmail(dto, 1)
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
        if (dto.phoneNumber.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Phone Number required"); return@put
        }
        users.upsertUserByEmail(dto, 1)

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

    post("add/usage/logs") {

        val dto = call.receive<AppUsageLogDto>()

        if (dto.userId <= 0) {

            call.respond(
                HttpStatusCode.BadRequest,
                "valid userId required"
            )

            return@post
        }

        users.insertAppUsageLog(dto)

        call.respond(
            HttpStatusCode.Created
        )
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
                UserNotificationFactory.passwordUpdated(
                    userId = user.userId.toString(),
                    email = user.emailId,
                    phone = user.phoneNumber?.let { "91$it" },
                    userName = user.fullName ?: "User"
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

        call.safeRoute(
            logMessage = "GET /users/feedback/all failed",
            clientMessage = "Failed to fetch feedback"
        ) {

            users.getAllFeedbacks()
        }
    }

    get("feedback/all/usergroup") {

        call.safeRoute(
            logMessage = "GET /users/feedback/all/usergroup failed",
            clientMessage = "Failed to fetch feedback"
        ) {
            users.getFeedbackWithUserGroup()
        }
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

    post("/otp/send") {

        var req: SendOtpReq? = null
        val ip = call.clientIp()

        try {

            req = call.receive<SendOtpReq>()

            if (
                !RateLimiters.resendOtpLimiter.allow("OTP_IP:$ip") ||
                !RateLimiters.resendOtpLimiter.allow("OTP_USER:${req.userId}")
            ) {
                return@post call.respondError(
                    HttpStatusCode.TooManyRequests,
                    "rate_limit",
                    "Please wait before requesting another code."
                )
            }

            val expiresAtMillis =
                otpService.sendOtp(
                    userId = req.userId,
                    phoneNumber = req.phoneNumber,
                    purpose = req.purpose
                )

            call.respondOk(
                SendOtpResp(
                    otpExpiresAtMillis = expiresAtMillis
                )
            )

        } catch (e: Exception) {

            call.application.log.error(
                "otp send failed for userId=${req?.userId ?: "unknown"}",
                e
            )

            call.respondError(
                HttpStatusCode.InternalServerError,
                "internal",
                "Failed to send OTP"
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

        call.safeRoute(
            logMessage = "GET /users/is-email-verified/$userId failed",
            clientMessage = "Failed to check email verification status"
        ) {
            emailVerificationService.isEmailVerified(userId)
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

    post("/otp/verify") {

        try {

            val req = call.receive<VerifyOtpReq>()

            otpService.verifyOtp(
                otp = req.otp,
                userId = req.userId,
                purpose = req.purpose,
                channel = req.channel
            )

            call.respondOk(true)

        } catch (e: BadRequestException) {

            call.respondError(
                HttpStatusCode.BadRequest,
                "validation",
                e.message ?: "Invalid or expired OTP"
            )

        } catch (e: Exception) {

            call.application.log.error(
                "otp verify failed",
                e
            )

            call.respondError(
                HttpStatusCode.InternalServerError,
                "internal",
                "OTP verification failed"
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

        // Force correct identity
        val user = existing.first()

        val updatedDto = user.copy(
            firebaseUserId = req.firebaseUserId
        )

        // Reuse existing UPSERT
        users.upsertUserByEmail(
            updatedDto,
            1
        ) // update is done by the system hence hardcoding to 1 for now.

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

inline suspend fun <reified T> ApplicationCall.safeRoute(
    logMessage: String,
    clientMessage: String,
    block: suspend () -> T
) {
    try {
        respondOk(block())
    } catch (e: Exception) {

        application.log.error(logMessage, e)

        respondError(
            HttpStatusCode.InternalServerError,
            "internal",
            clientMessage
        )
    }
}

