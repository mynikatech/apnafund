package com.mynikatech.apnafund.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mynikatech.apnafund.data.model.UserNotifications

@Dao
interface NotificationsDao {

    @Insert
    suspend fun addNotifications(userNotifications: UserNotifications)

    @Update
    suspend fun updateNotifications(userNotifications: UserNotifications)

    @Delete
    suspend fun deleteNotifications(userNotifications: UserNotifications)

    @Query("SELECT * from user_notifications where userId = :userId")
    suspend fun getUserNotifications(userId: Int): List<UserNotifications>?
}