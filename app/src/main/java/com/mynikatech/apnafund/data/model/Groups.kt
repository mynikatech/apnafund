package com.mynikatech.apnafund.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.mynikatech.apnafund.constants.ApnaBankConstants


@Entity(tableName = "groups",
        foreignKeys = [
        ForeignKey(
        entity = Users::class,
        parentColumns = ["userId"],
        childColumns = ["moderator"],
        onDelete = ForeignKey.CASCADE
    )
])
data class Groups(

    @PrimaryKey(autoGenerate = true) val groupId: Int = 0,
    val groupName: String,
    val moderator: Int? = null,
    val createdDate: String,
    val description: String?,
    val groupCode: String,
    val status: String = ApnaBankConstants.STATUS_ACTIVE
)
