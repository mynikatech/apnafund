package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.data.model.GroupMembers
import com.mynikatech.apnafund.net.dto.GroupsDto
import com.mynikatech.apnafund.net.dto.GroupMemberWithNameDto
import com.mynikatech.apnafund.net.dto.GroupMembersDto

interface GroupsApi {
    suspend fun getAllGroups(): List<GroupsDto>
    suspend fun getGroup(id: Int): GroupsDto?
    suspend fun addGroup(dto: GroupsDto): Int
    suspend fun updateGroup(id: Int, dto: GroupsDto): Boolean
    suspend fun deleteGroup(id: Int): Boolean
    suspend fun getGroupByMod(moderator: Int): GroupsDto

    // members
    suspend fun getAllMembersofGroup(groupId: Int): List<GroupMembersDto>
    suspend fun addGroupMember(groupMembers: GroupMembers): Int
    suspend fun updateGroupMember(groupMembers: GroupMembers)
    suspend fun hasModerator(groupId: Int): Boolean
    suspend fun deleteGroupMember(groupMembers: GroupMembers): Boolean
    suspend fun getAllMembersofGroupWithNames( groupId: Int): List<GroupMemberWithNameDto>
    suspend fun getGroupMembersWithNamesForFund(fundId: Int): List<GroupMemberWithNameDto>
    suspend fun getGroupMembersForFund(fundId: Int): List<GroupMembersDto>
    suspend fun checkIfGroupMemberAlreadyAdded(userId: Int, groupId: Int): Boolean
    suspend fun getTotMemberNumbersForFund(groupId: Int): Int

    suspend fun syncFirebaseUid(userId: Int, groupId: Int, firebaseUid: String)
}



