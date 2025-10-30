package com.mynikatech.apnafund.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mynikatech.apnafund.data.model.Type

@Dao
interface TypeDao {

    @Insert
    suspend fun addType(type: Type)

    @Query("SELECT * from type")
    fun getAllTypes(): List<Type>

    @Update
    suspend fun updateType(type: Type)

    @Delete
    suspend fun deleteType(type: Type)



}