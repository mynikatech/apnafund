package com.mynikatech.apnafund.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mynikatech.apnafund.data.model.GroupMemberWithName
import com.mynikatech.apnafund.data.model.GroupMembers
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupMembersDao {

    @Insert
    suspend fun addGroupMember(groupMembers: GroupMembers)

    @Query("SELECT * from group_members WHERE groupId = :groupId ")
    suspend fun getAllMembersofGroup( groupId: Int): List<GroupMembers>


    @Update
    suspend fun updateGroupMember(groupMembers: GroupMembers)

    @Delete
    suspend fun deleteGroupMember(groupMembers: GroupMembers)


    // Get GroupMembers for a given Fund
    @Query("""
        SELECT gm.*
        FROM group_members AS gm
        INNER JOIN funds AS f ON gm.groupId = f.groupId
        WHERE f.fundId = :fundId
    """)
    fun getGroupMembersForFund(fundId: Int): Flow<List<GroupMembers>>

    @Query("""SELECT CAST(COUNT(1) AS BIT)
            FROM group_members
            WHERE userId = :userId
            AND groupId = :groupId""")
    suspend fun checkIfGroupMemberAlreadyAdded(userId: Int, groupId: Int): Boolean

    @Query("""
        SELECT count(gm.groupMemberId)
        FROM group_members AS gm
        WHERE gm.groupId= :groupId
    """)
    suspend fun getTotMemberNumbersForFund(groupId: Int): Int

}