package com.mynikatech.apnafund.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "fund_members",
        foreignKeys = [
            ForeignKey(
                entity = Funds::class,
                parentColumns =["fundId"],
                childColumns = ["fundId"],
                onDelete = ForeignKey.CASCADE
            ),
            ForeignKey(
                entity = Users::class,
                parentColumns =["userId"],
                childColumns = ["userId"],
                onDelete = ForeignKey.CASCADE
            )

        ]
)
data class FundMembers(

    @PrimaryKey(autoGenerate = true) val fundMemberId: Int = 0,
    val userId: Int,
    val fundId: Int,
    val joiningDate: String,
    val role: String = "MEMBER",
    val status: String = "ACTIVE",
    val updatedAt: Long? = null,
    val updatedBy: Int? = null
)
