package com.mynikatech.apnafund.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mynikatech.apnafund.constants.ApnaBankConstants

@Entity( tableName = "privilege")
data class Privilege(
    @PrimaryKey(autoGenerate = true) val privilegeId: Int = 0,
    val privilegeCode: String,
    val privilegeDescription: String?,
    val status: String = ApnaBankConstants.STATUS_ACTIVE
)
