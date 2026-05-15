package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.data.model.GroupMembers
import com.mynikatech.apnafund.net.dto.AddMemberRequest
import com.mynikatech.apnafund.net.dto.GroupMemberWithNameDto
import com.mynikatech.apnafund.net.dto.GroupMembersDto
import com.mynikatech.apnafund.net.dto.GroupsDto
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import android.util.Log
import com.mynikatech.apnafund.data.mappers.toDto
import com.mynikatech.apnafund.net.dto.FirebaseSyncRequest
import com.mynikatech.apnafund.net.dto.GroupCreationRequest
import com.mynikatech.apnafund.net.dto.GroupsWithModeratorDto
import com.mynikatech.apnafund.session.SessionManager

class GroupsApiKtor(
    private val clientProvider: () -> io.ktor.client.HttpClient = { HttpClientProvider.client }
) : GroupsApi {

    private val client get() = clientProvider()

    override suspend fun getAllGroups(): List<GroupsDto> =
        client.get("/groups/get/all").unwrap<List<GroupsDto>>()

    override suspend fun getAllGroupsWithModeratorIndo(): List<GroupsWithModeratorDto> =
        client.get("/groups/get/all/moderator-info").unwrap<List<GroupsWithModeratorDto>>()

    override suspend fun getGroup(id: Int): GroupsDto? =
        client.get("/groups/get/$id").unwrap<GroupsDto>()

    override suspend fun addGroup(dto: GroupsDto): GroupsDto =
        client.post("/groups/add") {
            contentType(ContentType.Application.Json)
            setBody(GroupCreationRequest(
                group = dto,
                requestorId = SessionManager.userId
            ))
        }.unwrap<GroupsDto>()

    override suspend fun updateGroup(id: Int, dto: GroupsDto): Boolean {
        val resp = client.put("/groups/update/$id") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
        return resp.status.value in 200..299
    }

    override suspend fun deleteGroup(id: Int): Boolean =
        client.delete("/groups/delete/$id").unwrap<Boolean>()

    override suspend fun getGroupByMod(moderator: Int): GroupsDto =
        client.get("/groups/get/bymod").unwrap<GroupsDto>()

    override suspend fun getAllMembersofGroup(groupId: Int): List<GroupMembersDto> =
        client.get("/groups/get/members/$groupId").unwrap<List<GroupMembersDto>>()

    override suspend fun addGroupMember(groupMembers: GroupMembers): Int =
        client.post("/groups/add/member/${groupMembers.groupId}") {
            contentType(ContentType.Application.Json)
            setBody(
                AddMemberRequest(
                    userId = groupMembers.userId,
                    joiningDate = groupMembers.joiningDate.toString(),
                    role = groupMembers.role,
                    requestorId = groupMembers.updatedBy ?: 1 // in case of any issue it will be updated by system
                )
            )
        }.unwrap<Int>()

    override suspend fun hasModerator(groupId: Int): Boolean =
        client.get("/groups/has-moderator/$groupId").unwrap<Boolean>()

    override suspend fun deleteGroupMember(groupMember: GroupMembers): Boolean =
        client.delete("/groups/delete/member") {
            contentType(ContentType.Application.Json)
            setBody(groupMember)
        }.unwrap<Boolean>()

    override suspend fun updateGroupMember(groupMembers: GroupMembers) {
        client.put("/groups/update/member") {
            contentType(ContentType.Application.Json)
            setBody(groupMembers.toDto())
        }.body<Unit>()
    }

    override suspend fun getAllMembersofGroupWithNames(
        groupId: Int,
        onlyActive: Boolean
    ): List<GroupMemberWithNameDto> =
        client.get("/groups/get/members/with-names/$groupId") {
            parameter("onlyActive", onlyActive)
        }.unwrap()

    override suspend fun getGroupMembersWithNamesForFund(fundId: Int): List<GroupMemberWithNameDto> =
        client.get("/groups/get/members/fund/with-names/$fundId")
            .unwrap<List<GroupMemberWithNameDto>>()

    override suspend fun getGroupMembersForFund(fundId: Int): List<GroupMembersDto> =
        client.get("/groups/get/members/fund/$fundId").unwrap<List<GroupMembersDto>>()


    override suspend fun checkIfGroupMemberAlreadyAdded(userId: Int, groupId: Int): Boolean =
        client.get("/groups/members/check/$groupId") {
            parameter("userId", userId)
        }.unwrap<Boolean>()

    override suspend fun getTotMemberNumbersForFund(groupId: Int): Int =
        client.get("/groups/members/count/$groupId")
            .unwrap<Int>()

    override suspend fun syncFirebaseUid(userId: Int, groupId: Int, firebaseUid: String) {
        Log.d("FireBase Chat","Calling syncFirebaseUid")
        val groupIds = SessionManager.userGroups
            ?.mapNotNull { it.groupId }
            ?: emptyList()

        if (groupIds.isEmpty()) {
            Log.d("Firebase", "Skipping Firebase sync - no groups")
            return
        }

        val request = FirebaseSyncRequest(
            userId = SessionManager.userId,
            firebaseUid = SessionManager.firebaseUid,
            groupIds = groupIds
        )

        client.post("/groups/sync/firebase-uid") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }


}
