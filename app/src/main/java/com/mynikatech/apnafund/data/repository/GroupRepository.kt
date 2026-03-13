package com.mynikatech.apnafund.data.repository

import com.mynikatech.apnafund.data.dao.GroupMembersDao
import com.mynikatech.apnafund.data.dao.GroupsDao
import com.mynikatech.apnafund.data.mappers.toDto
import com.mynikatech.apnafund.data.mappers.toEntity
import com.mynikatech.apnafund.data.model.GroupMemberWithName
import com.mynikatech.apnafund.data.model.GroupMembers
import com.mynikatech.apnafund.data.model.Groups
import com.mynikatech.apnafund.net.GroupsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class GroupRepository(
    private val groupsDao: GroupsDao, private val groupMembersDao: GroupMembersDao,
    private val api: GroupsApi
) {


    /** Pull from server and upsert into Room (naive merge). */
    /* suspend fun refreshGroups(): Unit = withContext(Dispatchers.IO) {
        val remote = api.getAllGroups()
        val local = groupsDao.getAllGroupsOnce()
        val remoteIds = remote.mapNotNull { it.groupId }.toSet()

        // delete locals no longer present on server
        local.filter { it.groupId !in remoteIds }
            .forEach { groupsDao.deleteGroup(it) }

        // upsert all remotes
        remote.forEach { dto ->
            val id = dto.groupId
            val entity = dto.toEntity().copy(groupId = id ?: 0)
            val exists = id?.let { groupsDao.getGroupOrNull(it) } != null
            if (exists) groupsDao.updateGroup(entity) else groupsDao.addGroup(entity)
        }
    } */

    suspend fun refreshGroups(): Unit = withContext(Dispatchers.IO) {
        runCatching { api.getAllGroups() }.getOrNull()
    }

    /** Update remotely, then update Room. */
    suspend fun updateGroupRemoteAndCache(group: Groups): Boolean = withContext(Dispatchers.IO) {
        val id = requireNotNull(group.groupId) { "groupId required" }
        val ok = api.updateGroup(id, group.toDto())
        // if (ok) groupsDao.updateGroup(group)
        ok
    }

    /** Delete remotely, then remove from Room. */
    suspend fun deleteGroupRemoteAndCache(groupId: Int): Boolean = withContext(Dispatchers.IO) {
        val ok = api.deleteGroup(groupId)
        //if (ok) groupsDao.getGroupOrNull(groupId)?.let { groupsDao.deleteGroup(it) }
        ok
    }

    suspend fun createGroup(group: Groups) {
        api.addGroup(group.toDto())
    }

    suspend fun deleteGroup(group: Groups) {
        api.deleteGroup(group.groupId)
    }

    suspend fun updateGroup(group: Groups) {
        api.updateGroup(group.groupId, group.toDto())
    }

    fun fetchAllGroups(): Flow<List<Groups>> =
        flow {
            val dtos = api.getAllGroups()
            emit(dtos.map { it.toEntity() })
        }
            .catch { emit(emptyList()) }
            .flowOn(Dispatchers.IO)

    suspend fun fetchGroup(groupId: Int): Groups? {
        return api.getGroup(groupId)?.toEntity()
    }
    suspend fun fetchGroupByMod(moderator: Int): Groups {
        return api.getGroupByMod(moderator).toEntity()
    }

    suspend fun createGroupMember(groupMembers: GroupMembers) {
        api.addGroupMember(groupMembers)
    }

    suspend fun fetchAllMembersforGroup(groupId: Int): List<GroupMembers> {
        return api.getAllMembersofGroup(groupId).toEntity()
    }

    suspend fun fetchAllMembersofGroupWithNames(groupId: Int): List<GroupMemberWithName> {
        return api.getAllMembersofGroupWithNames(groupId).toEntity()
    }

    suspend fun getGroupMembersForFund(fundId: Int): Flow<List<GroupMembers>> =
        flow {
            val dtos = api.getGroupMembersForFund(fundId)
            emit(dtos.map { it.toEntity() })
        }
            .catch { emit(emptyList()) }
            .flowOn(Dispatchers.IO)

    suspend fun getGroupMembersWithNamesForFund(fundId: Int): List<GroupMemberWithName> {
        return api.getGroupMembersWithNamesForFund(fundId).toEntity()
    }

    suspend fun checkIfGroupMemberAlreadyAdded(userId: Int, groupId: Int): Boolean {
        return api.checkIfGroupMemberAlreadyAdded(userId, groupId)
    }

    suspend fun getTotMemberNumbersForFund(groupId: Int): Int {
        return api.getTotMemberNumbersForFund(groupId)
    }

    suspend fun syncFirebaseUid(
        userId: Int,
        groupId: Int,
        firebaseUid: String
    ) {
        api.syncFirebaseUid(
            userId = userId,
            groupId = groupId,
            firebaseUid = firebaseUid
        )
    }


}