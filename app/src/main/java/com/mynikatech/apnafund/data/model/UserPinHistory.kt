package com.mynikatech.apnafund.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_pin_history",
    foreignKeys = [ForeignKey(
        entity = Users::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class UserPinHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val pinHash: String,
    val changedAt: Long = System.currentTimeMillis()
)