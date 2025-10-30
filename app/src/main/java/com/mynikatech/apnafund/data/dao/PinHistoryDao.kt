package com.mynikatech.apnafund.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mynikatech.apnafund.data.model.UserPinHistory

@Dao
interface PinHistoryDao {

    @Insert
    suspend fun insertPINHistory(entry: UserPinHistory)

    @Query("SELECT pinHash FROM user_pin_history WHERE userId = :userId ORDER BY changedAt DESC LIMIT 3")
    suspend fun getLast3PINHashes(userId: Int): List<String>

    @Query("SELECT changedAt FROM user_pin_history WHERE userId = :userId ORDER BY changedAt DESC LIMIT 1")
    suspend fun getLastPINChangeDate(userId: Int): Long?
}
