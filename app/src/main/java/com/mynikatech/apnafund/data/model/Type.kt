package com.mynikatech.apnafund.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity( tableName = "type",
        indices = [Index(value = ["typeCode"], unique = true)]
)
data class Type(
    @PrimaryKey(autoGenerate = true) val typeId: Int = 0,
    val typeCode: String,
    val typeDescription: String,
    val status: String = "ACTIVE"

)
