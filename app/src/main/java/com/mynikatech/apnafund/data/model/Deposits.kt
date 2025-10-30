package com.mynikatech.apnafund.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "deposits",
    foreignKeys = [
        ForeignKey(
            entity = Funds::class,
            parentColumns = ["fundId"],
            childColumns = ["fundId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Deposits(
    @PrimaryKey(autoGenerate = true) val depositId: Int = 0,
    val depositorId: Int,
    val depositedDate: String,
    val depositAmount: Double,
    val depositMonth: String,
    val depositYear: String,
    val fundId: Int,
    val lateFee: Double?
)
