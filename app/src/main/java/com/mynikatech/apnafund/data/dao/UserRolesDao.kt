package com.mynikatech.apnafund.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mynikatech.apnafund.data.model.PendingModeratorRequest
import com.mynikatech.apnafund.data.model.Roles
import com.mynikatech.apnafund.data.model.UserProfile
import com.mynikatech.apnafund.data.model.UserRoles
import kotlinx.coroutines.flow.Flow

@Dao
interface UserRolesDao {

    @Insert
    suspend fun addUserRole(userRoles: UserRoles)


    suspend fun addUserRoles(userRoles: List<UserRoles>){
        userRoles.forEach { userRole ->
            addUserRole(userRole)
        }
    }

    @Query("""SELECT r.roleCode from user_roles ur
            JOIN roles r ON r.roleId = ur.roleId
            JOIN users u ON u.userId = ur.userId
            WHERE u.userId = :userId
        """)
    suspend fun getAllUserRoles(userId: Int): List<String>

    @Query("""SELECT r.roleId from user_roles ur
            JOIN roles r ON r.roleId = ur.roleId
            JOIN users u ON u.userId = ur.userId
            WHERE u.userId = :userId
            AND r.status = "ACTIVE"
        """)
    suspend fun getAllUserRoleIds(userId: Int): List<Int>

    @Query("SELECT * from user_roles WHERE roleId = :roleId")
    fun getAllUsersForARole(roleId: Int): Flow<List<UserRoles>>

    @Update
    suspend fun updateUserRoles(userRoles: UserRoles)

    @Query("SELECT * from ROLES")
    suspend fun getAllRoles(): List<Roles>

    @Delete
    suspend fun deleteUserRoles(userRoles: UserRoles)

    @Query("""SELECT 
        u.userId, 
        u.firstName || ' ' || IFNULL(u.lastName, '') AS userName,
        r.roleId, 
        r.roleCode,
        (
            SELECT gm.groupId
            FROM group_members gm
            WHERE gm.userId = u.userId
            LIMIT 1
        ) AS groupId,
        (
            SELECT g.groupName
            FROM group_members gm
            JOIN `groups` g ON g.groupId = gm.groupId
            WHERE gm.userId = u.userId
            LIMIT 1
        ) AS groupCode,
        u.isPinSet,
        u.firstName,
        u.lastName,
        u.emailId,
        u.phoneNumber
    FROM users u
    JOIN user_roles ur ON u.userId = ur.userId
    JOIN roles r ON r.roleId = ur.roleId
    WHERE u.userId = :userId;
        """)
    suspend fun getUserProfile(userId: Int): List<UserProfile>

    @Query("""
    SELECT u.userId, g.groupId, 
           u.firstName || ' ' || u.lastName AS moderatorName, 
           g.groupName, 
           g.description AS groupDescription
    FROM USERS u
    INNER JOIN USER_ROLES ur ON u.userId = ur.userId
    INNER JOIN ROLES r ON ur.roleId = r.roleId
    INNER JOIN 'GROUPS' g ON g.moderator = u.userId
    WHERE r.roleCode = 'MODERATOR'
      AND ur.status = 'PENDING'
      AND g.status = 'PENDING'
""")
    suspend fun getPendingModeratorRequests(): List<PendingModeratorRequest>

}