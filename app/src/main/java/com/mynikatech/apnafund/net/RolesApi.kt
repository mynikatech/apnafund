package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.net.dto.PrivilegeDto
import com.mynikatech.apnafund.net.dto.RolesDto
import com.mynikatech.apnafund.net.dto.RolePrivilegeDto
import com.mynikatech.apnafund.net.dto.RoleWithPrivilegesDto

interface RolesApi {
    // Roles
    suspend fun getAllRoles(): List<RolesDto>
    suspend fun getRole(id: Int): RolesDto?
    suspend fun addRole(dto: RolesDto): Int
    suspend fun updateRole(id: Int, dto: RolesDto): Boolean
    suspend fun deleteRole(id: Int): Boolean
    suspend fun getRoleIdByRoleCode(code: String): Int
    suspend fun assignRole(userId: Int, roleId: Int): Boolean

    // Privileges
    suspend fun listPrivileges(): List<PrivilegeDto>
    suspend fun getPrivilege(id: Int): PrivilegeDto?
    suspend fun createPrivilege(dto: PrivilegeDto): Int
    suspend fun updatePrivilege(id: Int, dto: PrivilegeDto): Boolean
    suspend fun deletePrivilege(id: Int): Boolean

    // Role ↔ Privilege mapping
    suspend fun listRolePrivileges(roleId: Int): List<PrivilegeDto>
    suspend fun addRolePrivilege(dto: RolePrivilegeDto): Int
    suspend fun removeRolePrivilege(rolePrivilegeId: Int): Boolean
    suspend fun getPrivilegesGroupedByRole():List<RoleWithPrivilegesDto>
    suspend fun getAllRolesWithPrivileges(): List<RoleWithPrivilegesDto>
}
