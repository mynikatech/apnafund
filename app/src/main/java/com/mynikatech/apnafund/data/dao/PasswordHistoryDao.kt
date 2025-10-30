package com.mynikatech.apnafund.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mynikatech.apnafund.data.model.UserPasswordHistory

@Dao
interface PasswordHistoryDao {

    @Insert
    suspend fun insertPasswordHistory(entry: UserPasswordHistory)

    @Query("SELECT passwordHash FROM user_password_history WHERE userId = :userId ORDER BY changedAt DESC LIMIT 3")
    suspend fun getLast3PasswordHashes(userId: Int): List<String>

    @Query("SELECT changedAt FROM user_password_history WHERE userId = :userId ORDER BY changedAt DESC LIMIT 1")
    suspend fun getLastPasswordChangeDate(userId: Int): Long?
}
