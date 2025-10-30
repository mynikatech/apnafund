package com.mynikatech.apnafund.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feedbacks")
data class Feedback(
    @PrimaryKey(autoGenerate = true) val feedbackId: Int = 0,
    val userId: Int,
    val message: String,
    val timestamp: String
)
