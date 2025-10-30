package com.mynikatech.apnafund.server.roles

import com.mynikatech.apnafund.net.dto.*
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.kotlin.BindKotlin
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlQuery

@RegisterKotlinMapper(RolesDto::class)
@RegisterKotlinMapper(PrivilegeDto::class)
@RegisterKotlinMapper(RolePrivilegeDto::class)
@RegisterKotlinMapper(UserRolesDto::class)
interface RolesSql {

    // ---- Roles ----
    @SqlQuery("""SELECT * FROM get_roles()""")
    fun listRoles(): List<RolesDto>

    @SqlQuery("""SELECT * FROM get_role(:id)""")
    fun getRole(@Bind("id") id: Int): List<RolesDto>

    @SqlQuery("""SELECT create_role(:roleCode,:roleDescription,:status)""")
    fun createRole(@BindKotlin r: RolesDto): Int

    @SqlQuery("""SELECT update_role(:id,:roleCode,:roleDescription,:status)""")
    fun updateRole(@Bind("id") id: Int, @BindKotlin r: RolesDto): Boolean

    @SqlQuery("""SELECT delete_role(:id)""")
    fun deleteRole(@Bind("id") id: Int): Boolean

    @SqlQuery("""SELECT get_role_id_by_code(:code)""")
    fun getRoleIdByRoleCode(@Bind("code") code: String): Int?

    // ---- Privileges ----
    @SqlQuery("""SELECT * FROM get_privileges()""")
    fun listPrivileges(): List<PrivilegeDto>

    @SqlQuery("""SELECT * FROM get_privilege(:id)""")
    fun getPrivilege(@Bind("id") id: Int): List<PrivilegeDto>

    @SqlQuery("""SELECT create_privilege(:privilegeCode,:privilegeDescription,:status)""")
    fun createPrivilege(@BindKotlin p: PrivilegeDto): Int

    @SqlQuery("""SELECT update_privilege(:id,:privilegeCode,:privilegeDescription,:status)""")
    fun updatePrivilege(@Bind("id") id: Int, @BindKotlin p: PrivilegeDto): Boolean

    @SqlQuery("""SELECT delete_privilege(:id)""")
    fun deletePrivilege(@Bind("id") id: Int): Boolean

    // ---- Role ↔ Privileges ----
    @SqlQuery("""SELECT * FROM get_privileges_of_role(:roleId)""")
    fun listRolePrivileges(@Bind("roleId") roleId: Int): List<PrivilegeDto>

    @SqlQuery("""SELECT create_role_privilege(:roleId,:privilegeId,:status)""")
    fun addRolePrivilege(@BindKotlin rp: RolePrivilegeDto): Int

    @SqlQuery("""SELECT delete_role_privilege(:rolePrivilegeId)""")
    fun removeRolePrivilege(@Bind("rolePrivilegeId") rolePrivilegeId: Int): Boolean

    // ---- User ↔ Roles (assign) ----
    @SqlQuery("""SELECT * FROM get_user_roles_of_user(:userId)""")
    fun getUserRolesOfUser(@Bind("userId") userId: Int): List<UserRolesDto>

    @SqlQuery("""SELECT add_user_role(:userId,:roleId,:status)""")
    fun addUserRole(@BindKotlin ur: UserRolesDto): Int

    @SqlQuery("""SELECT remove_user_role(:userRoleId)""")
    fun removeUserRole(@Bind("userRoleId") userRoleId: Int): Boolean

    @SqlQuery("""SELECT * FROM get_all_roles_with_privilege()""")
    fun getAllRolesWithPrivilege(): List<RoleWithPrivilegesDto>
}
