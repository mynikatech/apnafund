package com.mynikatech.apnafund.session

import android.content.Context
import android.content.SharedPreferences
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.net.dto.UserFundMembership
import com.mynikatech.apnafund.net.dto.UserGroup
import com.mynikatech.apnafund.net.dto.UserGroupMembership

object SessionManager {
    var userId: Int = -1
    var userName: String = ""
    var firstName: String = ""
    var lastName: String = ""
    var emailId: String? = null
    var phoneNumber: String ? = null
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
    var userPendingGroups: List<UserGroup>? = emptyList()
    var userGroupMemberships: List<UserGroupMembership>? = emptyList()
    var userFundMemberships: List<UserFundMembership>? = emptyList()

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

    fun updateGroupMembership(
        updated: UserGroupMembership
    ) {

        val memberships = userGroupMemberships ?: return

        val index = memberships.indexOfFirst {
            it.groupId == updated.groupId
        }

        if (index != -1) {

            userGroupMemberships =
                memberships.toMutableList().apply {
                    this[index] = updated
                }
        }
    }

    fun updateFundMembership(
        updated: UserFundMembership
    ) {

        val memberships = userFundMemberships ?: return

        val index = memberships.indexOfFirst {
            it.fundId == updated.fundId
        }

        if (index != -1) {

            userFundMemberships =
                memberships.toMutableList().apply {
                    this[index] = updated
                }
        }
    }

    fun addFundMembership(newMembership: UserFundMembership) {

        val memberships = userFundMemberships.orEmpty()

        val alreadyExists = memberships.any {
            it.fundId == newMembership.fundId
        }

        if (!alreadyExists) {
            userFundMemberships = memberships + newMembership
        }
    }

    fun addGroupMembership(newMembership: UserGroupMembership) {

        val memberships = userGroupMemberships.orEmpty()

        val alreadyExists = memberships.any {
            it.groupId == newMembership.groupId
        }

        if (!alreadyExists) {
            userGroupMemberships = memberships + newMembership
        }
    }

    fun canManageUsers(): Boolean {

        if (isAdmin()) return true

        return userGroupMemberships.orEmpty().any {

            it.status == ApnaBankConstants.STATUS_ACTIVE &&
                    (
                            it.role == "PRIMARY_MODERATOR" ||
                                    it.role == "MODERATOR"
                            )
        }
    }

    fun canManageGroups(): Boolean {

        if (isAdmin()) return true

        return userGroupMemberships.orEmpty().any {

            it.status == ApnaBankConstants.STATUS_ACTIVE &&
                    (
                            it.role == "PRIMARY_MODERATOR" ||
                                    it.role == "MODERATOR"
                            )
        }
    }

    fun canManageDeposits(): Boolean {

        if (isAdmin()) return true

        return userFundMemberships.orEmpty().any {

            it.status == ApnaBankConstants.STATUS_ACTIVE &&
                    (
                            it.role == "PRIMARY_MODERATOR" ||
                                    it.role == "MODERATOR"
                            )
        }
    }

    fun canManageLoanEmi(): Boolean {

        if (isAdmin()) return true

        return userFundMemberships.orEmpty().any {

            it.status == ApnaBankConstants.STATUS_ACTIVE &&
                    (
                            it.role == "PRIMARY_MODERATOR" ||
                                    it.role == "MODERATOR"
                            )
        }
    }

    fun canViewPendingApprovals(): Boolean {

        if (isAdmin()) return true

        val hasGroupModerationAccess =
            userGroupMemberships.orEmpty().any {

                it.status == ApnaBankConstants.STATUS_ACTIVE &&
                        (
                                it.role == "PRIMARY_MODERATOR" ||
                                        it.role == "MODERATOR"
                                )
            }

        val hasFundModerationAccess =
            userFundMemberships.orEmpty().any {

                it.status == ApnaBankConstants.STATUS_ACTIVE &&
                        (
                                it.role == "PRIMARY_MODERATOR" ||
                                        it.role == "MODERATOR"
                                )
            }

        return hasGroupModerationAccess ||
                hasFundModerationAccess
    }

    fun canManageFeedback(): Boolean {
        return isAdmin()
    }

    fun canManageFund(fundId: Int): Boolean {

        if (isAdmin()) return true

        return userFundMemberships.orEmpty().any {

            it.fundId == fundId &&
                    it.status == ApnaBankConstants.STATUS_ACTIVE &&
                    (
                            it.role == "PRIMARY_MODERATOR" ||
                                    it.role == "MODERATOR"
                            )
        }
    }
    fun canManageGroup(
        groupId: Int
    ): Boolean {

        if (isAdmin()) return true

        return userGroupMemberships.orEmpty().any {

            it.groupId == groupId &&
                    it.status == ApnaBankConstants.STATUS_ACTIVE &&
                    (
                            it.role == "PRIMARY_MODERATOR" ||
                                    it.role == "MODERATOR"
                            )
        }
    }

    fun isPrimaryFundModerator(
        fundId: Int
    ): Boolean {

        if (isAdmin()) return true

        return userFundMemberships.orEmpty().any {

            it.fundId == fundId &&
                    it.status == ApnaBankConstants.STATUS_ACTIVE &&
                    it.role == "PRIMARY_MODERATOR"
        }
    }

    fun isPrimaryGroupModerator(
        groupId: Int
    ): Boolean {

        if (isAdmin()) return true

        return userGroupMemberships.orEmpty().any {

            it.groupId == groupId &&

                    it.status == ApnaBankConstants.STATUS_ACTIVE &&

                    it.role == "PRIMARY_MODERATOR"
        }
    }


}