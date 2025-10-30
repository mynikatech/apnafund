package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.data.model.UserRoles
import com.mynikatech.apnafund.net.dto.PendingModeratorRequestDto
import com.mynikatech.apnafund.net.dto.RolesDto
import com.mynikatech.apnafund.net.dto.UserProfileDto
import com.mynikatech.apnafund.net.dto.UserRolesDto
import kotlinx.coroutines.flow.Flow

interface UserRolesApi {
    // Roles
    suspend fun listRolesOfUser(userId: Int): List<RolesDto>
    suspend fun listUserRolesOfUser(userId: Int): List<UserRolesDto>
    suspend fun addUserRoles(userRoles: List<UserRolesDto>): Int
    suspend fun removeUserRole(userRoleId: Int): Boolean
    suspend fun addUserRole(userRoles: UserRoles)
    suspend fun getAllUserRoles(userId: Int): List<String>
    suspend fun getAllUserRoleIds(userId: Int): List<Int>
    fun getAllUsersForARole(roleId: Int): Flow<List<UserRoles>>
    suspend fun updateUserRoles(userRoles: UserRoles)
    suspend fun deleteUserRoles(userRoles: UserRoles)
    suspend fun getUserProfile(userId: Int): List<UserProfileDto>
    suspend fun getPendingModeratorRequests(): List<PendingModeratorRequestDto>

}