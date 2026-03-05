package com.mynikatech.apnafund.session

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray

class PreferencesHelper(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun saveSession(
        userId: Int,
        userName: String,
        roleIds: List<Int>,
        roleNames: List<String>,
        groupId: Int? = null,
        token: String? = null,
        isPinSet: Boolean = false,
        firstName: String,
        lastName: String,
        emailId: String,
        phoneNumber: String,
        firebaseUid: String,
        groupName: String
    ) {
        prefs.edit().apply {
            putInt("user_id", userId)
            putString("user_name", userName)
            putString("role_ids", JSONArray(roleIds).toString())
            putString("role_names", JSONArray(roleNames).toString())
            putInt("group_id", groupId ?: -1)
            putString("token", token)
            putString("groupName", groupName)
            putBoolean("is_pin_set", isPinSet)
            putString("first_name", firstName)
            putString("last_name", lastName)
            putString("email_id", emailId)
            putString("phone_number", phoneNumber)
            putString("firebaseUid", firebaseUid)
            apply()
        }

        SessionManager.userId = userId
        SessionManager.userName = userName
        SessionManager.roleIds = roleIds
        SessionManager.roleNames = roleNames
        SessionManager.groupId = groupId
        SessionManager.token = token
        SessionManager.isPinSet = isPinSet
        SessionManager.firstName = firstName
        SessionManager.lastName = lastName
        SessionManager.emailId = emailId
        SessionManager.phoneNumber = phoneNumber
        SessionManager.firebaseUid = firebaseUid
        SessionManager.groupName = groupName
    }

    fun loadSession() {
        SessionManager.userId = prefs.getInt("user_id", -1)
        SessionManager.userName = prefs.getString("user_name", "") ?: ""
        SessionManager.roleIds =
            parseJsonArray(prefs.getString("role_ids", "[]") ?: "[]").map { it.toInt() }
        SessionManager.roleNames = parseJsonArray(prefs.getString("role_names", "[]") ?: "[]")
        val groupIdStored = prefs.getInt("group_id", -1)
        SessionManager.groupId = if (groupIdStored != -1) groupIdStored else null
        SessionManager.token = prefs.getString("token", null)
        SessionManager.isLoggedIn = SessionManager.userId > 0
        SessionManager.isPinSet = prefs.getBoolean("is_pin_set", false)
        SessionManager.firstName = prefs.getString("first_name", "") ?: ""
        SessionManager.lastName = prefs.getString("last_name", "") ?: ""
        SessionManager.emailId = prefs.getString("email_id", "") ?: ""
        SessionManager.phoneNumber = prefs.getString("phone_number", "") ?: ""
        SessionManager.firebaseUid = prefs.getString("firebaseUid", "") ?: ""
        SessionManager.groupName = prefs.getString("groupName", "") ?: ""


    }

    fun clearSession() {
        prefs.edit { clear() }
        SessionManager.clearSession()
    }

    private fun parseJsonArray(json: String): List<String> {
        val jsonArray = JSONArray(json)
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.optString(i))
        }
        return list
    }

    fun isPinSet(): Boolean {
        val value = prefs.getBoolean("is_pin_set", false)
        return value
    }

    fun isLoggedIn(): Boolean {
        return prefs.getInt("user_id", -1) > 0
    }
    fun getUserId(): Int {
        val value = prefs.getInt("user_id", -1)
        return value
    }

}
