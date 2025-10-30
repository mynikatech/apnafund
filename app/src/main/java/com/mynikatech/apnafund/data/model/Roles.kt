package com.mynikatech.apnafund.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mynikatech.apnafund.constants.ApnaBankConstants

@Entity( tableName = "roles")
data class Roles(
    @PrimaryKey(autoGenerate = true) val roleId: Int = 0,
    val roleCode: String,
    val roleDescription: String?,
    val status: String = ApnaBankConstants.STATUS_ACTIVE
)
