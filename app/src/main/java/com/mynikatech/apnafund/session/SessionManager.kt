package com.mynikatech.apnafund.session

import android.content.Context
import android.content.SharedPreferences
import com.mynikatech.apnafund.net.dto.UserGroup

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
    var groupName: String? = null
    var isLoggedIn: Boolean = false
    var isPinSet: Boolean = false
    var firebaseUid: String = ""
    var firebaseToken: String? = null
    private lateinit var prefs: SharedPreferences
    var isFirebaseSynced: Boolean = false
    var userGroups: List<UserGroup>? = emptyList()

    val hasValidSession: Boolean
        get() = userId > 0

    fun init(context: Context) {
        prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    }

    fun isMultiGroupUser(): Boolean {
        return (userGroups?.size ?: 0) > 1
    }

    fun setSelectedGroup(group: UserGroup) {
        groupId = group.groupId
        groupName = group.groupName
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
        groupName = null
        token = null
        isLoggedIn = false
        isPinSet = false
        firebaseUid = ""
        firebaseToken = ""
    }

    private fun hasRole(role: String): Boolean {
        return roleNames.any { it.equals(role, ignoreCase = true) }
    }

    fun isAdmin() = hasRole("ADMIN")
    fun isModerator() =
        hasRole("GROUP_MODERATOR") || hasRole("FUND_MODERATOR")

    fun isGroupModerator() = hasRole("GROUP_MODERATOR")
    fun isFundModerator() = hasRole("FUND_MODERATOR")

    fun getFormattedUserName(): String {
        return "${firstName} ${lastName}"
    }


}