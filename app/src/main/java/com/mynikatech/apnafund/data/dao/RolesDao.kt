package com.mynikatech.apnafund.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mynikatech.apnafund.data.model.RoleWithPrivileges
import com.mynikatech.apnafund.data.model.Roles
import kotlinx.coroutines.flow.Flow

@Dao
interface RolesDao {

    @Insert
    suspend fun addRole(role: Roles)

    @Query("SELECT * from roles")
    fun getAllUsers(): Flow<List<Roles>>

    @Update
    suspend fun updateUser(role: Roles)

    @Delete
    suspend fun deleteUser(role: Roles)

    @Query("""
    SELECT r.roleId, r.roleCode, p.privilegeCode
    FROM roles r
    INNER JOIN role_privilege rp ON r.roleId = rp.roleId
    INNER JOIN privilege p ON rp.privilegeId = p.privilegeId
    WHERE r.status = 'ACTIVE' AND rp.status = 'ACTIVE' AND p.status = 'ACTIVE'
""")
    suspend fun getAllRolesWithPrivileges(): List<RoleWithPrivileges>

    @Query("SELECT * from ROLES")

    suspend fun getAllRoles(): List<Roles>

    @Query("SELECT roleId from ROLES WHERE roleCode = :roleCode")
    suspend fun getRoleIdByRoleCode(roleCode: String): Int

}