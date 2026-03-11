package com.mynikatech.apnafund.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mynikatech.apnafund.ApnaFundApplication
import com.mynikatech.apnafund.data.model.GroupMemberWithName
import com.mynikatech.apnafund.data.model.GroupMembers
import com.mynikatech.apnafund.data.model.Groups
import com.mynikatech.apnafund.net.dto.UserGroup
import com.mynikatech.apnafund.util.ApnaBankDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class GroupViewModel : ViewModel() {

    private val groupRepository = ApnaFundApplication.groupRepository
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val groups: StateFlow<List<Groups>> =
        groupRepository.fetchAllGroups() // or repo.fetchAllGroups()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun groupsForUser(isAdmin: Boolean, moderatorGroupId: Int = 0): Flow<List<Groups>> =
        if (isAdmin) groups
        else groups.map { list -> list.filter { it.groupId == moderatorGroupId } }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                groupRepository.refreshGroups()
            } catch (t: Throwable) {
                _error.value = t.message ?: "Refresh failed"
            } finally {
                _loading.value = false
            }
        }
    }
    fun createOrUpdateGroupRemote(group: Groups) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                if (group.groupId == 0) {
                    groupRepository.createGroupRemoteAndCache(group)
                } else {
                    groupRepository.updateGroupRemoteAndCache(group)
                }
            } catch (t: Throwable) {
                _error.value = t.message ?: "Save failed"
            } finally {
                _loading.value = false
            }
        }
    }
    fun deleteGroupRemote(groupId: Int) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                groupRepository.deleteGroupRemoteAndCache(groupId)
            } catch (t: Throwable) {
                _error.value = t.message ?: "Delete failed"
            } finally {
                _loading.value = false
            }
        }
    }

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

    suspend fun fetchAllGroups(): Flow<List<Groups>> {
        val groups = groupRepository.fetchAllGroups()
        return groups
    }

    suspend fun fetchAllGroups(isAdmin: Boolean, userId: Int): Flow<List<Groups>> {
        return if (!isAdmin) {
            val group = groupRepository.fetchGroup(userId)!!
            flowOf(listOf(group))
        } else {
            groupRepository.fetchAllGroups()
        }
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

    fun saveOrUpdateGroup(group: Groups) {
        viewModelScope.launch {
            try {
          if (group.groupId == 0) {
                    groupRepository.createGroup(group)
                } else {
                    groupRepository.updateGroup(group)
                }
            } catch (t: Throwable) {
                _error.value = t.message ?: "Local save failed"
            }
        }
    }
}