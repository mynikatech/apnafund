package com.mynikatech.apnafund.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.model.GroupMembers
import com.mynikatech.apnafund.util.ApnaBankDate

@Dao
interface AdminDao {

    @Transaction
    suspend fun approveModeratorAndGroup(userId: Int, roleId: Int, groupId: Int) {
        updateUserRoleStatus(userId, roleId, ApnaBankConstants.STATUS_ACTIVE)
        updateGroupStatus(groupId, ApnaBankConstants.STATUS_ACTIVE)
        // Moderator is added as a member to the Group when it is approved
        val groupMember = GroupMembers(
            userId = userId,
            groupId = groupId,
            joiningDate = ApnaBankDate.getCurrentDate()
        )
        addGroupMember(groupMember)
    }

    @Transaction
    suspend fun rejectModeratorAndGroup(userId: Int, roleId: Int, groupId: Int) {
        updateUserRoleStatus(userId, roleId, ApnaBankConstants.STATUS_REJECTED)
        updateGroupStatus(groupId, ApnaBankConstants.STATUS_REJECTED)
    }

    @Query("UPDATE USER_ROLES SET status = :status WHERE userId = :userId AND roleId = :roleId")
    suspend fun updateUserRoleStatus(userId: Int, roleId: Int, status: String)

    @Query("UPDATE 'GROUPS' SET status = :status WHERE groupId = :groupId")
    suspend fun updateGroupStatus(groupId: Int, status: String)

    @Insert
    suspend fun addGroupMember(groupMembers: GroupMembers)


}