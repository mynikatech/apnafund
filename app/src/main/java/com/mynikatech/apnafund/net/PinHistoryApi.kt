package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.data.model.UserPinHistory

interface PinHistoryApi {
    suspend fun insertPINHistory(entry: UserPinHistory)


    suspend fun getLast3PINHashes(userId: Int): List<String>


    suspend fun getLastPINChangeDate(userId: Int): Long?
}