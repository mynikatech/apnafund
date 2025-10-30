package com.mynikatech.apnafund.server.admin

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate

interface AdminSql {

    // Procedures (no return)

    @SqlUpdate("""CALL pr_approve_moderator_and_group(:userId,:roleId,:groupId)""")
    fun approve(@Bind("userId") userId: Int, @Bind("roleId") roleId: Int, @Bind("groupId") groupId: Int)

    @SqlUpdate("""CALL pr_reject_moderator_and_group(:userId,:roleId,:groupId)""")
    fun reject(@Bind("userId") userId: Int, @Bind("roleId") roleId: Int, @Bind("groupId") groupId: Int)

    @SqlUpdate("""CALL approve_moderator_and_group(:userId,:roleId,:groupId)""")
    fun approveModeratorAndGroup(@Bind("userId") userId: Int,
                                 @Bind("roleId") roleId: Int,
                                 @Bind("groupId") groupId: Int)

    @SqlUpdate("""CALL reject_moderator_and_group(:userId,:roleId,:groupId)""")
    fun rejectModeratorAndGroup(@Bind("userId") userId: Int,
                                @Bind("roleId") roleId: Int,
                                @Bind("groupId") groupId: Int)

    @SqlUpdate("""CALL update_user_role_status(:userId,:roleId,:status)""")
    fun updateUserRoleStatus(@Bind("userId") userId: Int,
                             @Bind("roleId") roleId: Int,
                             @Bind("status") status: String)

    @SqlUpdate("""CALL update_group_status(:groupId,:status)""")
    fun updateGroupStatus(@Bind("groupId") groupId: Int,
                          @Bind("status") status: String)

    // Function (returns new/existing groupMemberId)
    @SqlQuery("""SELECT add_group_member_admin(:userId,:groupId, CAST(:joiningDate AS date))""")
    fun addGroupMember(@Bind("userId") userId: Int,
                       @Bind("groupId") groupId: Int,
                       @Bind("joiningDate") joiningDate: String): Int
}

