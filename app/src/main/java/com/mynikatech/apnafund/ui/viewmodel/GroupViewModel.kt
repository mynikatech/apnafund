package com.mynikatech.apnafund.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mynikatech.apnafund.ApnaFundApplication
import com.mynikatech.apnafund.data.model.GroupMemberWithName
import com.mynikatech.apnafund.data.model.GroupMembers
import com.mynikatech.apnafund.data.model.Groups
import com.mynikatech.apnafund.net.dto.GroupsWithModeratorDto
import com.mynikatech.apnafund.util.ApnaBankDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class GroupViewModel : ViewModel() {

    private val groupRepository = ApnaFundApplication.groupRepository
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val groups: StateFlow<List<Groups>> =
        groupRepository.fetchAllGroups() // or repo.fetchAllGroups()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun createGroupMember(memberId: Int, groupId: Int) {
        val groupMember = GroupMembers(
            userId = memberId,
            groupId = groupId,
            joiningDate = ApnaBankDate.getCurrentDate()
        )
        viewModelScope.launch {
            groupRepository.createGroupMember(groupMember)
        }
    }

    fun fetchAllGroups(): Flow<List<Groups>> {
        val groups = groupRepository.fetchAllGroups()
        return groups
    }

    fun fetchAllGroupsWithModerator(): Flow<List<GroupsWithModeratorDto>> {
        val groups = groupRepository.fetchAllGroupsWithModerator()
        return groups
    }

    suspend fun fetchGroup(groupId: Int): Groups {
        val group = groupRepository.fetchGroup(groupId)!!
        return group
    }

    suspend fun fetchGroupMembersforGrp(groupId: Int): List<GroupMembers> {
        val groupMembers = groupRepository.fetchAllMembersforGroup(groupId)
        return groupMembers
    }

    suspend fun fetchGroupMembersforGrpWithNames(groupId: Int): List<GroupMemberWithName> {
        val groupMembers = groupRepository.fetchAllMembersofGroupWithNames(groupId)
        return groupMembers
    }

    suspend fun checkIfGroupMemberAlreadyAdded(userId: Int, groupId: Int): Boolean {
        return groupRepository.checkIfGroupMemberAlreadyAdded(userId, groupId)
    }

    suspend fun saveOrUpdateGroup(group: Groups) {
        if (group.groupId == 0) {
            groupRepository.createGroup(group)
        } else {
            groupRepository.updateGroup(group)
        }
    }

}