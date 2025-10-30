package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.data.model.UserPasswordHistory

interface PasswordHistoryApi {

    suspend fun insertPasswordHistory(entry: UserPasswordHistory)

    suspend fun getLast3PasswordHashes(userId: Int): List<String>

    suspend fun getLastPasswordChangeDate(userId: Int): Long?

}