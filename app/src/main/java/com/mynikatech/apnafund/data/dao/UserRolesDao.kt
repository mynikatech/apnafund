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


    @Query("""
    SELECT
        u."userId",
        g."groupId",
        g."groupName",
        u."firstName" || ' ' || u."lastName" AS "moderatorName",
        u."emailId",
        g."description" AS "groupDescription"
    FROM "users" u
    INNER JOIN "user_roles" ur ON u."userId" = ur."userId"
    INNER JOIN "roles" r       ON ur."roleId" = r."roleId"
    INNER JOIN "groups" g      ON g."moderator" = u."userId"
    WHERE r."roleCode" = 'MODERATOR'
      AND ur."status" = 'PENDING'
      AND g."status"  = 'PENDING';
""")
    suspend fun getPendingModeratorRequests(): List<PendingModeratorRequest>

}