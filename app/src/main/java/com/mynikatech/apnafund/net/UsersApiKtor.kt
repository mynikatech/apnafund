package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.net.dto.FundWithDetailsDto
import com.mynikatech.apnafund.net.dto.FundsDto
import com.mynikatech.apnafund.net.dto.GroupMembersDto
import com.mynikatech.apnafund.net.dto.GroupsDto
import com.mynikatech.apnafund.net.dto.UserDetailsDto
import com.mynikatech.apnafund.net.dto.UserFundDetailsDto
import com.mynikatech.apnafund.net.dto.UserNotificationsDto
import com.mynikatech.apnafund.net.dto.UserPasswordHistoryDto
import com.mynikatech.apnafund.net.dto.UserPinHistoryDto
import com.mynikatech.apnafund.net.dto.UserProfileDto
import com.mynikatech.apnafund.net.dto.UserWithGroupDto
import com.mynikatech.apnafund.net.dto.UsersDto
import com.mynikatech.apnafund.net.dto.ValidateUserRequest
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

    override suspend fun getUserByEmail(email: String): UsersDto? =
        try {
            client.get("/users/get/by-email") { parameter("email", email) }.unwrap<UsersDto>()
        } catch (_: ClientRequestException) {
            null
        }

    override suspend fun getUserByPhone(phone: String): UsersDto? =
        try {
            client.get("/users/get/by-phone") { parameter("phone", phone) }.unwrap<UsersDto>()
        } catch (_: ClientRequestException) {
            null
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

    override suspend fun getUserProfile(userId: Int): List<UserProfileDto>? =
        try {
            client.get("/users/get/profile/$userId").unwrap<List<UserProfileDto>>()
        } catch (_: ApiException) {
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }

    override suspend fun updateUserPassword(id: Int, passwordHash: String): Boolean {
        val response: HttpResponse = client.put("/users/update/$id/password") {
            contentType(ContentType.Application.Json)
            setBody(UserPasswordHistoryDto(passwordHash = passwordHash, userId = id))
        }
        return response.status == HttpStatusCode.NoContent || response.status == HttpStatusCode.OK
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

    override suspend fun getUserDetails(userId: Int): UserDetailsDto =
        client.get("/users/get/details/$userId").unwrap<UserDetailsDto>()

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
}
