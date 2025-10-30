package com.mynikatech.apnafund.data.model

data class UserDetails(

    val firstName: String,
    val lastName: String?,
    val emailId: String?,
    val phoneNumber: String?,
    val status: String,
    val userCode: String,
    val userRoles: List<String>,
    val userNotifications: List<UserNotifications>,
    val userFunds: List<Funds>,
    val group: Groups?
)
