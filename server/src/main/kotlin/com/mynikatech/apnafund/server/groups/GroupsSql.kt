package com.mynikatech.apnafund.server.groups

import com.mynikatech.apnafund.net.dto.GroupMemberWithNameDto
import com.mynikatech.apnafund.net.dto.GroupMembersDto
import com.mynikatech.apnafund.net.dto.GroupsDto
import com.mynikatech.apnafund.net.dto.GroupsWithModeratorDto
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.kotlin.BindKotlin
import org.jdbi.v3.sqlobject.statement.SqlQuery

@RegisterKotlinMapper(GroupsDto::class)
@RegisterKotlinMapper(GroupMembersDto::class)
@RegisterKotlinMapper(GroupMemberWithNameDto::class)
interface GroupsSql {

    // -------- Groups --------

    @SqlQuery("""SELECT * FROM get_groups()""")
    fun getAllGroups(): List<GroupsDto>

    @SqlQuery("""SELECT * FROM get_groups_with_moderator_info()""")
    fun getAllGroupsWithModeratorInfo(): List<GroupsWithModeratorDto>

    @SqlQuery("""SELECT * FROM get_group(:id)""")
    fun getGroup(@Bind("id") id: Int): List<GroupsDto>   // returns 0..1 row; caller can firstOrNull()

    @SqlQuery("""
        SELECT * from add_group(
            :groupName,
            :description,
            :moderator,
            :groupCode,
            :createdDate,
            :status
        )
    """)
    fun addGroup(@BindKotlin g: GroupsDto): GroupsDto

    @SqlQuery("""
        SELECT update_group(
            :id,
            :groupName,
            :description,
            :moderator,
            :groupCode,
            :status
        )
    """)
    fun updateGroup(
        @Bind("id") id: Int,
        @Bind("groupName") groupName: String?,
        @Bind("description") description: String?,
        @Bind("moderator") moderator: Int?,
        @Bind("groupCode") groupCode: String?,
        @Bind("status") status: String?
    ): Boolean

    @SqlQuery("""SELECT delete_group(:id)""")
    fun deleteGroup(@Bind("id") id: Int): Boolean

    @SqlQuery("""SELECT * FROM get_group_by_moderator(:moderator)""")
    fun getGroupByMod(@Bind("moderator") moderatorId: Int): List<GroupsDto> // 0..1 row

    @SqlQuery("""SELECT does_group_have_moderator(:groupId)""")
    fun hasModerator(@Bind("groupId") groupId: Int): Boolean


    // -------- Members (group) --------

    @SqlQuery("""SELECT * FROM get_group_members(:groupId)""")
    fun getAllMembersofGroup(@Bind("groupId") groupId: Int): List<GroupMembersDto>

    @SqlQuery("""SELECT add_group_member(:userId, :groupId, CAST(:joiningDate AS date))""")
    fun addGroupMember(
        @Bind("userId") userId: Int,
        @Bind("groupId") groupId: Int,
        @Bind("joiningDate") joiningDate: String // yyyy-MM-dd
    ): Int

    // NOTE: this assumes update_group_member RETURNS boolean.
    // If yours currently RETURNS void, see the note below.
    @SqlQuery("""
        SELECT update_group_member(
            :groupMemberId,
            :userId,
            :groupId,
            CAST(:joiningDate AS date)
        )
    """)
    fun updateGroupMember(@BindKotlin gm: GroupMembersDto): Boolean

    @SqlQuery("""SELECT delete_group_member(:groupMemberId, :userId, :groupId)""")
    fun deleteGroupMember(@BindKotlin gm: GroupMembersDto): Boolean

    @SqlQuery("""SELECT * FROM get_group_members_with_names(:groupId)""")
    fun getAllMembersofGroupWithNames(@Bind("groupId") groupId: Int): List<GroupMemberWithNameDto>

    @SqlQuery("""SELECT group_member_exists(:userId, :groupId)""")
    fun checkIfGroupMemberAlreadyAdded(
        @Bind("userId") userId: Int,
        @Bind("groupId") groupId: Int
    ): Boolean

    @SqlQuery("""SELECT count_group_members(:groupId)""")
    fun getTotMemberNumbersForFund(@Bind("groupId") groupId: Int): Int


    // -------- Members (fund-based) --------

    @SqlQuery("""SELECT * FROM get_members_with_names_for_fund(:fundId)""")
    fun getGroupMembersWithNamesForFund(@Bind("fundId") fundId: Int): List<GroupMemberWithNameDto>

    @SqlQuery("""SELECT * FROM get_group_members_for_fund(:fundId)""")
    fun getGroupMembersForFundOnce(@Bind("fundId") fundId: Int): List<GroupMembersDto>

    @SqlQuery("""
    SELECT activate_group(:groupId)
    """)
    fun activateGroup(
        @Bind("groupId") groupId: Int
    ): Boolean

    @SqlQuery("""
    SELECT reject_group(:groupId)
    """)
    fun rejectGroup(
        @Bind("groupId") groupId: Int
    ): Boolean
}
