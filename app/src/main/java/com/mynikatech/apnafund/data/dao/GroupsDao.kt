package com.mynikatech.apnafund.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mynikatech.apnafund.data.model.Groups
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupsDao {

    @Insert
    suspend fun addGroup(group: Groups)

    @Query("SELECT * from  'groups' WHERE status = 'ACTIVE'")
    fun getAllGroups(): Flow<List<Groups>>

    @Query("SELECT * from  'groups' WHERE groupId = :groupId LIMIT 1 ")
    suspend fun getGroup(groupId: Int): Groups

    @Query("SELECT * from  'groups' WHERE moderator = :moderator LIMIT 1 ")
    fun getGroupByMod(moderator: Int): Groups

    @Update
    suspend fun updateGroup(group: Groups)

    @Delete
    suspend fun deleteGroup(group: Groups)

    @Query("SELECT * from  'groups'")
    suspend fun getAllGroupsOnce(): List<Groups>

    @Query("SELECT * from  'groups' WHERE groupId = :id LIMIT 1")
    suspend fun getGroupOrNull(id: Int): Groups?
}