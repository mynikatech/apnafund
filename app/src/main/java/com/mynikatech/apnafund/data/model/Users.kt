package com.mynikatech.apnafund.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.util.ApnaBankDate

@Entity ( tableName = "users")
data class Users(
    @PrimaryKey(autoGenerate = true) val userId: Int = 0,
    val firstName: String,
    val lastName: String?,
    val emailId: String?,
    val phoneNumber: String?,
    val status: String = ApnaBankConstants.STATUS_ACTIVE,
    var passwordHash: String? = null, // hashed version of password
    val createdDate: String = ApnaBankDate.getCurrentDate(),
    val isPinSet: Boolean = false,
    val hashPIN: String? = null,
    val firebaseUserId: String? = null,
    val userCode: String  // Construct as first 2 letters of FirstName+ 2 letters of lastname+ random 5 digit number with trailing zeroes
)
