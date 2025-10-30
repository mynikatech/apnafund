package com.mynikatech.apnafund.session

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    var userId: Int = -1
    var userName: String = ""
    var firstName: String = ""
    var lastName: String = ""
    var emailId: String = ""
    var phoneNumber: String = ""
    var roleIds: List<Int> = emptyList()
    var roleNames: List<String> = emptyList()
    var groupId: Int? = null
    var token: String? = null
    var isLoggedIn: Boolean = false
    var isPinSet: Boolean = false
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    }

    fun clearSession() {
        userId = -1
        userName = ""
        firstName = ""
        lastName = ""
        emailId = ""
        phoneNumber = ""
        roleIds = emptyList()
        roleNames = emptyList()
        groupId = null
        token = null
        isLoggedIn = false
        isPinSet = false
    }

    private fun hasRole(role: String): Boolean {
        return roleNames.any { it.equals(role, ignoreCase = true) }
    }

    fun isAdmin() = hasRole("ADMIN")
    fun isModerator() = hasRole("MODERATOR")

    fun getFormattedUserName(): String {
        return "${firstName} ${lastName}"
    }
}