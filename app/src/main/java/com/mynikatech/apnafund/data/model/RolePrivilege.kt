package com.mynikatech.apnafund.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.mynikatech.apnafund.constants.ApnaBankConstants

@Entity(tableName = "role_privilege",
    foreignKeys = [
        ForeignKey(
            entity = Roles::class,
            parentColumns =["roleId"],
            childColumns = ["roleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Privilege::class,
            parentColumns =["privilegeId"],
            childColumns = ["privilegeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RolePrivilege(

    @PrimaryKey(autoGenerate = true) val rolePrivilegeId: Int = 0,
    val privilegeId: Int,
    val roleId: Int,
    val status: String = ApnaBankConstants.STATUS_ACTIVE
)
