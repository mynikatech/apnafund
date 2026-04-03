package com.mynikatech.apnafund.server.userroles

import com.mynikatech.apnafund.net.dto.PendingModeratorRequestDto
import com.mynikatech.apnafund.net.dto.RolesDto
import com.mynikatech.apnafund.net.dto.UserRolesDto
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlQuery

@RegisterKotlinMapper(RolesDto::class)
@RegisterKotlinMapper(UserRolesDto::class)
@RegisterKotlinMapper(PendingModeratorRequestDto::class)
interface UserRolesSql {

    // Reads
    @SqlQuery("""SELECT * FROM get_roles_of_user(:userId)""")
    fun getRolesOfUser(@Bind("userId") userId: Int): List<RolesDto>

    @SqlQuery("""SELECT * FROM get_user_roles_of_user(:userId)""")
    fun getUserRolesOfUser(@Bind("userId") userId: Int): List<UserRolesDto>

    @SqlQuery("""SELECT * FROM get_all_user_role_codes(:userId)""")
    fun getAllUserRoleCodes(@Bind("userId") userId: Int): List<String>

    @SqlQuery("""SELECT * FROM get_all_user_role_ids(:userId)""")
    fun getAllUserRoleIds(@Bind("userId") userId: Int): List<Int>

    @SqlQuery("""SELECT * FROM get_all_users_for_role(:roleId)""")
    fun getAllUsersForRole(@Bind("roleId") roleId: Int): List<UserRolesDto>

    @SqlQuery("""SELECT * FROM get_pending_moderator_requests()""")
    fun getPendingModeratorRequests(): List<PendingModeratorRequestDto>

    // Writes
    @SqlQuery("""SELECT add_user_role(:userId,:roleId,:status)""")
    fun addUserRole(
        @Bind("userId") userId: Int,
        @Bind("roleId") roleId: Int,
        @Bind("status") status: String
    ): Int

    @SqlQuery("""SELECT add_user_roles_batch(CAST(:items AS jsonb))""")
    fun addUserRoles(@Bind("items") itemsJson: String): Int

    @SqlQuery("""SELECT update_user_role(:userRoleId,:status)""")
    fun updateUserRole(
        @Bind("userRoleId") userRoleId: Int,
        @Bind("status") status: String
    ): Boolean

    @SqlQuery("""SELECT remove_user_role(:userRoleId)""")
    fun removeUserRole(@Bind("userRoleId") userRoleId: Int): Boolean

    @SqlQuery("""SELECT delete_user_role_by_composite(:userId,:roleId)""")
    fun deleteUserRoleByComposite(
        @Bind("userId") userId: Int,
        @Bind("roleId") roleId: Int
    ): Boolean


}