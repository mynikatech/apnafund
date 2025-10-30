package com.mynikatech.apnafund.net


interface AdminApi {

    suspend fun approveModeratorAndGroup(userId: Int, roleId: Int, groupId: Int)
    suspend fun rejectModeratorAndGroup(userId: Int, roleId: Int, groupId: Int)
    suspend fun updateUserRoleStatus(userId: Int, roleId: Int, status: String)
    suspend fun updateGroupStatus(groupId: Int, status: String)
}