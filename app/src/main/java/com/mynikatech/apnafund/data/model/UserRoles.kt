package com.mynikatech.apnafund.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.mynikatech.apnafund.constants.ApnaBankConstants

@Entity(tableName = "user_roles",
        foreignKeys = [
            ForeignKey(
                entity = Users::class,
                parentColumns =["userId"],
                childColumns = ["userId"],
                onDelete = ForeignKey.CASCADE
            ),
            ForeignKey(
                entity = Roles::class,
                parentColumns =["roleId"],
                childColumns = ["roleId"],
                onDelete = ForeignKey.CASCADE
            )
        ]
)
data class UserRoles(
    @PrimaryKey(autoGenerate = true) val userRoleId: Int = 0,
    val userId: Int,
    val roleId: Int,
    val status: String = ApnaBankConstants.STATUS_ACTIVE
)
