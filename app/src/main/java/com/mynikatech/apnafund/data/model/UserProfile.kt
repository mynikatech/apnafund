package com.mynikatech.apnafund.data.model

data class UserProfile(
    var userId: Int,
    var userName: String,
    var roleId: Int,
    var roleCode: String,
    var groupId: Int?,
    var groupName: String?,
    var token: String? = null,
    var isLoggedIn: Boolean? = false,
    var isPinSet: Boolean? = false,
    var firstName: String,
    var lastName: String?,
    var emailId: String,
    var phoneNumber: String
)
