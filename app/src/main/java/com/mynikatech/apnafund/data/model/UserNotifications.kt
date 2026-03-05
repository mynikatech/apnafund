package com.mynikatech.apnafund.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_notifications",
    foreignKeys = [
        ForeignKey(
            entity = Users::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Type::class,
            parentColumns = ["typeCode"],
            childColumns = ["notificationType"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserNotifications(
    @PrimaryKey(autoGenerate = true) val userNotificationId: Int = 0,
    val notificationType: String,
    val userId: Int,
    val message: String?,
    val isExpiredFlag: Boolean = false,
    val publishedFlag: Boolean = false,
    val readFlag: Boolean = false,
    val status: String = "ACTIVE",
    val createdAt: String?,   // ISO string from DB
    val readAt: String?,
    val expiredAt: String?
)
