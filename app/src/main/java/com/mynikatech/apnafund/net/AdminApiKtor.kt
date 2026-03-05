package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.net.dto.ApproveRejectReq
import com.mynikatech.apnafund.net.dto.UpdateGroupStatusReq
import com.mynikatech.apnafund.net.dto.UpdateUserRoleStatusReq
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AdminApiKtor(
    private val clientProvider: () -> io.ktor.client.HttpClient = { HttpClientProvider.client }
) : AdminApi {

    private val client get() = clientProvider()

    override suspend fun approveModeratorAndGroup(userId: Int, roleId: Int, groupId: Int, moderatorName: String,moderatorEmail: String, groupName: String) {
        client.post("/admin/approve/moderator-and-group") {
            contentType(ContentType.Application.Json)
            setBody(ApproveRejectReq(userId = userId, roleId = roleId, groupId = groupId, userEmail = moderatorEmail, userName = moderatorName, groupName = groupName  ))
        }.body<Unit>()
    }

    override suspend fun rejectModeratorAndGroup(userId: Int, roleId: Int, groupId: Int, moderatorName: String,moderatorEmail: String, groupName: String) {
        client.post("/admin/reject/moderator-and-group") {
            contentType(ContentType.Application.Json)
            setBody(ApproveRejectReq(userId = userId, roleId = roleId, groupId = groupId, userEmail = moderatorEmail, userName = moderatorName, groupName = groupName))
        }.body<Unit>()
    }

    override suspend fun updateUserRoleStatus(userId: Int, roleId: Int, status: String) {
        client.put("/admin/update/user-role-status") {
            contentType(ContentType.Application.Json)
            setBody(UpdateUserRoleStatusReq( userId = userId, roleId = roleId, status = status))
        }.body<Unit>()
    }

    override suspend fun updateGroupStatus(groupId: Int, status: String) {
        client.put("/admin/update/group-status/$groupId") {
            contentType(ContentType.Application.Json)
            setBody(UpdateGroupStatusReq(status = status))
        }.body<Unit>()
    }

}
