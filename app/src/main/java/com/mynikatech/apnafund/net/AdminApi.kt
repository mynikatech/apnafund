package com.mynikatech.apnafund.net


interface AdminApi {

    suspend fun approveModeratorAndGroup(userId: Int, roleId: Int, groupId: Int, moderatorName: String,moderatorEmail: String, groupName: String)
    suspend fun rejectModeratorAndGroup(userId: Int, roleId: Int, groupId: Int, moderatorName: String,moderatorEmail: String, groupName: String)
    suspend fun updateUserRoleStatus(userId: Int, roleId: Int, status: String)
    suspend fun updateGroupStatus(groupId: Int, status: String)
}