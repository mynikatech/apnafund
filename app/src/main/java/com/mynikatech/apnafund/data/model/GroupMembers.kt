package com.mynikatech.apnafund.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "group_members",
        foreignKeys = [
            ForeignKey(
                entity = Groups::class,
                parentColumns =["groupId"],
                childColumns = ["groupId"],
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
data class GroupMembers(

    @PrimaryKey(autoGenerate = true) val groupMemberId: Int = 0,
    val userId: Int,
    val groupId: Int,
    val joiningDate: String
)
