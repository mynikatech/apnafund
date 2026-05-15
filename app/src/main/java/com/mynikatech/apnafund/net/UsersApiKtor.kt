package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.net.api.ApiResponse
import com.mynikatech.apnafund.net.dto.ChangePasswordRequest
import com.mynikatech.apnafund.net.dto.FirebaseTokenResp
import com.mynikatech.apnafund.net.dto.FundWithDetailsDto
import com.mynikatech.apnafund.net.dto.FundsDto
import com.mynikatech.apnafund.net.dto.GroupMembersDto
import com.mynikatech.apnafund.net.dto.GroupsDto
import com.mynikatech.apnafund.net.dto.GroupsWithModeratorDto
import com.mynikatech.apnafund.net.dto.LoginUserResponse
import com.mynikatech.apnafund.net.dto.ModeratorRegistrationResponse
import com.mynikatech.apnafund.net.dto.RegisterModeratorRequest
import com.mynikatech.apnafund.net.dto.RegisterOrUpdateUserRequest
import com.mynikatech.apnafund.net.dto.SaveOrUpdateUserResponse
import com.mynikatech.apnafund.net.dto.SendEmailVerificationReq
import com.mynikatech.apnafund.net.dto.SendEmailVerificationResp
import com.mynikatech.apnafund.net.dto.UnreadCountDto
import com.mynikatech.apnafund.net.dto.UpdateFirebaseUidReq
import com.mynikatech.apnafund.net.dto.UserDetailsDto
import com.mynikatech.apnafund.net.dto.UserFundDetailsDto
import com.mynikatech.apnafund.net.dto.UserNotificationsDto
import com.mynikatech.apnafund.net.dto.UserPinHistoryDto
import com.mynikatech.apnafund.net.dto.UserProfileDto
import com.mynikatech.apnafund.net.dto.UserWithGroupDto
import com.mynikatech.apnafund.net.dto.UsersDto
import com.mynikatech.apnafund.net.dto.ValidateUserRequest
import com.mynikatech.apnafund.net.dto.VerifyEmailReq
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType


class UsersApiKtor(
    private val clientProvider: () -> io.ktor.client.HttpClient = { HttpClientProvider.client }
) : UsersApi {

    private val client get() = clientProvider()

    // ---- CRUD ----

    override suspend fun getUsers(): List<UsersDto> =
        client.get("/users/get/all").unwrap<List<UsersDto>>()

    override suspend fun getUser(id: Int): UsersDto? =
        try {
            client.get("/users/get/$id").unwrap()
        } catch (_: ApiException) {
            null
        }
    override suspend fun getUsersForModerator(userId: Int): List<UsersDto> {
        return try {
            client.get("/users/get/for-moderator/$userId")
                .unwrap<List<UsersDto>>()
        } catch (e: Exception) {
            emptyList() // fallback
        }
    }
    override suspend fun addUser(user: UsersDto): Int =
        client.post("/users/add") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }.unwrap<Int>()

    override suspend fun updateUser(id: Int, user: UsersDto): Boolean =
        try {
            val res = client.put("/users/update/$id") {
                contentType(ContentType.Application.Json)
                setBody(user)
            }
            res.status == HttpStatusCode.NoContent || res.status == HttpStatusCode.OK
        } catch (_: Exception) {
            false
        }

    override suspend fun deleteUser(id: Int): Boolean =
        try {
            client.delete("/users/delete/$id").unwrap<Boolean>()
        } catch (_: Exception) {
            false
        }

    override suspend fun deleteAll(): Boolean =
        try {
            client.delete("/users/delete/all").unwrap<Boolean>()
        } catch (_: Exception) {
            false
        }

    // ---- lookups/checks ----

    override suspend fun doesUserExists(emailId: String): Boolean =
        client.get("/users/get/exists/by-email") { parameter("email", emailId) }.unwrap<Boolean>()

    override suspend fun countMatchingUsers(email: String, phone: String, excludeUserId: Int): Int =
        try {
            client.get("/users/get/count-matching") {
                parameter("email", email)
                parameter("phone", phone)
                parameter("excludeUserId", excludeUserId)
            }.unwrap<Int>()
        } catch (_: ApiException) {
            0
        }

    override suspend fun getUserByEmail(email: String): LoginUserResponse =
        safeApiCall {
            client.get("/users/get/by-email") {
                parameter("email", email)
            }
        }.unwrap()

    override suspend fun getUserByPhone(phone: String): LoginUserResponse =
        safeApiCall {
            client.get("/users/get/by-phone") {
                parameter("phone", phone)
            }
        }.unwrap()

    override suspend fun validateUserPasswordChange(
        userId: Int,
        password: String
    ) {
        safeApiCall {
            client.get("/users/check/password-change") {
                parameter("userId", userId)
                parameter("password", password)
            }
        }.unwrapNoContent()
    }

    override suspend fun validateUser(email: String, password: String): UsersDto? =
        try {
            client.post("/users/validate") {
                contentType(ContentType.Application.Json)
                setBody(ValidateUserRequest(emailId = email, passwordHash = password))
            }.unwrap<UsersDto>()
        } catch (_: ClientRequestException) {
            null
        }

    override suspend fun checkUserPIN(userId: Int, pin: String): Boolean =
        client.get("/users/check/pin") {
            parameter("userId", userId)
            parameter("pin", pin)
        }.unwrap<Boolean>()

    override suspend fun doesGroupHasModerator(groupId: Int): Boolean =
        client.get("/users/check/group-moderator") {
            parameter("groupId", groupId)
        }.unwrap<Boolean>()

    // ---- with group / profiles ----
    override suspend fun getUserWithGroup(groupId: Int?): List<UserWithGroupDto> =
        client.get("/users/get/with-group") { parameter("groupId", groupId) }
            .unwrap<List<UserWithGroupDto>>()

    override suspend fun getAllUsersWithGroup(): List<UserWithGroupDto> =
        client.get("/users/get/with-group/all").unwrap<List<UserWithGroupDto>>()

    override suspend fun getGroupMember(userId: Int, groupId: Int): GroupMembersDto? =
        try {
            val res = client.get("/users/get/group-member") {
                expectSuccess = false
                parameter("userId", userId)
                parameter("groupId", groupId)
            }
            if (res.status == HttpStatusCode.NotFound) {
                null
            } else {
                res.unwrap<GroupMembersDto>()
            }
        } catch (_: Exception) {
            null
        }

    override suspend fun getUserProfile(userId: Int): UserProfileDto? =
        try {
            client.get("/users/get/profile/$userId").unwrap<UserProfileDto>()
        } catch (_: ApiException) {
            null
        } catch (_: Exception) {
            null
        }

    override suspend fun updateUserPassword(id: Int, newPassword: String) {
        safeApiCall {
            client.post("/users/update/$id/password") {
                contentType(ContentType.Application.Json)
                setBody(ChangePasswordRequest(newPassword))
            }
        }.unwrapNoContent()
    }

    override suspend fun updateUserPIN(id: Int, pinHash: String): Boolean {
        val response: HttpResponse = client.put("/users/update/$id/pin") {
            contentType(ContentType.Application.Json)
            setBody(UserPinHistoryDto(pinHash = pinHash, userId = id))
        }
        return response.status == HttpStatusCode.NoContent || response.status == HttpStatusCode.OK
    }

    // ---- extras used by UI ----

    override suspend fun getGroupForUser(userId: Int): GroupsDto? =
        try {
            client.get("/users/get/group-for-user/$userId").unwrap<GroupsDto>()
        } catch (_: Exception) {
            null
        }


    override suspend fun getGroupsForUser(userId: Int): List<GroupsDto> =
        try {
            client.get("/users/get/groups-for-user/$userId").unwrap()
        } catch (_: Exception) {
            emptyList()
        }


    override suspend fun getGroupsForModeratorUser(userId: Int): List<GroupsDto> =
        try {
            client.get("/users/get/groups-for-moderator-user/$userId").unwrap()
        } catch (_: Exception) {
            emptyList()
        }

    override suspend fun getGroupsForModeratorUserWithModInfo(userId: Int): List<GroupsWithModeratorDto> =
        try {
            client.get("/users/get/groups-for-moderator-user/moderator-info/$userId").unwrap()
        } catch (_: Exception) {
            emptyList()
        }

    override suspend fun getFundsForUser(userId: Int): List<FundsDto> =
        try {
            client.get("/users/get/funds-for-user/$userId").unwrap<List<FundsDto>>()
        } catch (_: Exception) {
            emptyList()
        }

    override suspend fun getTotalDeposit(userId: Int, fundId: Int): Double? =
        try {
            client.get("/users/get/total-deposit") {
                parameter("userId", userId)
                parameter("fundId", fundId)
            }.unwrap<Double>()
        } catch (_: Exception) {
            null
        }

    override suspend fun getPerMemberExpectedMaturityAmount(fundId: Int): Double? =
        try {
            client.get("/users/get/per-member-expected-maturity-amount/$fundId")
                .unwrap<Double>()
        } catch (_: Exception) {
            null
        }

    override suspend fun getUserNotifications(userId: Int): List<UserNotificationsDto>? =
        try {
            client.get("/notifications/get/user/$userId").unwrap<List<UserNotificationsDto>>()
        } catch (_: Exception) {
            null
        }

    override suspend fun getUserDetails(userId: Int, groupId: Int?): UserDetailsDto {

        return if (groupId != null) {
            client.get("/users/get/details/$userId/$groupId").unwrap()
        } else {
            client.get("/users/details/$userId").unwrap()
        }
    }

    override suspend fun getTotalLoanAmount(userId: Int, fundId: Int): Double =
        client.get("/users/get/total-loan-amount") {
            parameter("userId", userId)
            parameter("fundId", fundId)
        }.unwrap<Double>()

    override suspend fun getFundWithDetails(fundId: Int): FundWithDetailsDto =
        client.get("/funds/get/with-details/$fundId").unwrap<FundWithDetailsDto>()

    override suspend fun getUserFundDetails(userId: Int, fundId: Int): UserFundDetailsDto? =
        try {
            client.get("/users/get/user-fund-details") {
                parameter("userId", userId)
                parameter("fundId", fundId)
            }.unwrap<UserFundDetailsDto>()
        } catch (_: Exception) {
            null
        }

    override suspend fun registerModeratorAndGroup(
        regModReq: RegisterModeratorRequest
    ): ModeratorRegistrationResponse {
        return client.post("/users/groups/register-moderator") {
            contentType(ContentType.Application.Json)
            setBody(regModReq)
        }.unwrap<ModeratorRegistrationResponse>()
    }

    override suspend fun registerOrUpdateUser(registerOrUpdateUserRequest: RegisterOrUpdateUserRequest): SaveOrUpdateUserResponse {
        return client.post("/users/register-update") {
            contentType(ContentType.Application.Json)
            setBody(registerOrUpdateUserRequest)
        }.unwrap<SaveOrUpdateUserResponse>()
    }

    override suspend fun verifyEmailOtp(req: VerifyEmailReq): Boolean {
        return client.post("/users/email/verify/confirm") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.unwrap<Boolean>()
    }

    override suspend fun sendEmailVerification(req: SendEmailVerificationReq): SendEmailVerificationResp {
        return client.post("/users/email/verify/send") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.unwrap<SendEmailVerificationResp>()
    }

    override suspend fun isEmailVerified(userId: Int): Boolean {
        return client.get("/users/is-email-verified/$userId")
            .unwrap<Boolean>()
    }

    override suspend fun updateFirebaseUserId(req: UpdateFirebaseUidReq): Boolean {
        val response = client.post("/users/firebase-uid/${req.userId}") {
            contentType(ContentType.Application.Json)
            setBody(
                mapOf("firebaseUserId" to req.firebaseUid)
            )
        }
        return response.status == HttpStatusCode.NoContent
    }
    override suspend fun getFirebaseTokenForUser(userId: Int): FirebaseTokenResp {
        return client.post("/users/firebase-token/$userId")
            .unwrap<FirebaseTokenResp>()
    }

    override suspend fun getUnreadNotificationCount(userId: Int): Int {
        return try {
            val response: HttpResponse =
                client.get("/notifications/users/$userId/unread-count")

            if (response.status == HttpStatusCode.OK) {

                val body: ApiResponse<UnreadCountDto> = response.body()

                body.data?.unreadCount ?: 0
            } else {
                0
            }

        } catch (e: Exception) {
            0
        }
    }

    override suspend fun markNotificationRead(notificationId: Int): Boolean {
        return try {
            val response: HttpResponse = client.post(
                "/notifications/users/$notificationId/read"
            )
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }
}
