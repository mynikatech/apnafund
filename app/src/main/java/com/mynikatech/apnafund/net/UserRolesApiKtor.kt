package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.data.model.UserRoles
import com.mynikatech.apnafund.net.dto.PendingModeratorRequestDto
import com.mynikatech.apnafund.net.dto.RolesDto
import com.mynikatech.apnafund.net.dto.UserProfileDto
import com.mynikatech.apnafund.net.dto.UserRolesDto
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class UserRolesApiKtor(
    private val clientProvider: () -> io.ktor.client.HttpClient = { HttpClientProvider.client }
) : UserRolesApi {

    private val client get() = clientProvider()

    override suspend fun listRolesOfUser(userId: Int): List<RolesDto> =
        client.get("/user-roles/get/roles/$userId").unwrap<List<RolesDto>>()

    override suspend fun listUserRolesOfUser(userId: Int): List<UserRolesDto> =
        client.get("/user-roles/get/user-roles/$userId").unwrap<List<UserRolesDto>>()

    override suspend fun getAllUserRoles(userId: Int): List<String> =
        client.get("/user-roles/get/all/$userId").unwrap<List<String>>()

    override suspend fun getAllUserRoleIds(userId: Int): List<Int> =
        client.get("/user-roles/get/all-ids/$userId").unwrap<List<Int>>()

    override fun getAllUsersForARole(roleId: Int): Flow<List<UserRoles>> = flow {
        val list: List<UserRoles> =
            client.get("/user-roles/get/users-for-role/$roleId").unwrap<List<UserRoles>>()
        emit(list)
    }

    override suspend fun getUserProfile(userId: Int): List<UserProfileDto> =
        client.get("/users/get/profile/$userId").unwrap<List<UserProfileDto>>()

    override suspend fun getPendingModeratorRequests(): List<PendingModeratorRequestDto> =
        client.get("/user-roles/get/pending-moderator-requests")
            .unwrap<List<PendingModeratorRequestDto>>()

    // ---- Mutations ----

    override suspend fun addUserRoles(userRoles: List<UserRolesDto>): Int =
        client.post("/user-roles/add/batch") {
            contentType(ContentType.Application.Json)
            setBody(userRoles)
        }.unwrap<Int>()

    override suspend fun removeUserRole(userRoleId: Int): Boolean =
        client.delete("/user-roles/delete/$userRoleId").unwrap<Boolean>()

    override suspend fun addUserRole(userRoles: UserRoles) {
        client.post("/user-roles/add") {
            contentType(ContentType.Application.Json)
            setBody(userRoles)
        }.body<Unit>()
    }

    override suspend fun updateUserRoles(userRoles: UserRoles) {
        client.put("/user-roles/update") {
            contentType(ContentType.Application.Json)
            setBody(userRoles)
        }.body<Unit>()
    }

    override suspend fun deleteUserRoles(userRoles: UserRoles) {
        client.delete("/user-roles/delete") {
            contentType(ContentType.Application.Json)
            setBody(userRoles)
        }.body<Unit>()
    }
}
