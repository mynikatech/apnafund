package com.mynikatech.apnafund.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.mynikatech.apnafund.ApnaFundApplication
import com.mynikatech.apnafund.data.model.GroupMembers
import com.mynikatech.apnafund.data.model.PendingModeratorRequest
import com.mynikatech.apnafund.data.model.UserRoles
import com.mynikatech.apnafund.data.model.UserWithGroup
import com.mynikatech.apnafund.data.model.Users
import com.mynikatech.apnafund.util.ApnaBankDate
import com.mynikatech.apnafund.util.Converters
import kotlinx.coroutines.flow.Flow

class UserViewModel : ViewModel() {

    private val userRolesRepository = ApnaFundApplication.userRolesRepository

    /** Flow source for the table (Room) */
    fun usersFlow(isAdmin: Boolean, moderatorGroupId: Int): Flow<List<UserWithGroup>> =
        if (isAdmin) userRolesRepository.getAllUsersWithGroup()
        else userRolesRepository.getUserWithGroup(moderatorGroupId)

    /** Pull latest users from the server and cache into Room */
    suspend fun refreshUsersAndCache() {
        userRolesRepository.refreshUsers()
    }

    /** Pull one user from server and cache */
    suspend fun refreshUserAndCache(id: Int) {
        userRolesRepository.refreshOne(id)
    }

    fun fetchUsers(): Flow<List<Users>> {
        val users = userRolesRepository.fetchAllUsers()
        return users
    }

    suspend fun fetchUser(userId: Int): Users? {
        val user = userRolesRepository.fetchUser(userId)
        return user
    }

    suspend fun doesUserExists(emailId: String): Boolean {
        val exists = userRolesRepository.doesUserExists(emailId)
        return exists
    }

    suspend fun saveOrUpdateUser(user: Users, groupId: Int = 0): Int {
        // 1) Create/Update on server, then cache in Room via the repo helpers
        val userId = try {
            if (user.userId == 0) {
                userRolesRepository.createUserRemoteAndCache(user)   // returns new id
            } else {
                userRolesRepository.updateUserRemoteAndCache(user)   // remote + local
                user.userId
            }
        } catch (t: Throwable) {
            if (user.userId == 0) {
                val id = userRolesRepository.createUserAndReturnId(user)
                id
            } else {
                userRolesRepository.updateUser(user.userId, user)
                user.userId
            }
        }

        // Ensure MEMBER role (remote + cache)
        userRolesRepository.ensureRoleRemoteAndCache(userId, roleCode = "MEMBER")

        // Optional: ensure group membership & moderator role
        if (groupId > 0) {
            userRolesRepository.ensureGroupMemberRemoteAndCache(
                GroupMembers(userId = userId, groupId = groupId, joiningDate = ApnaBankDate.getCurrentDate())
            )
            // If no moderator in this group, grant current user MODERATOR
            userRolesRepository.ensureModeratorIfNoneRemoteAndCache(userId, groupId)
        }

        return userId
    }

    suspend fun isDuplicate(email: String, phone: String, excludeUserId: Int = 0): Boolean {
        return userRolesRepository.isDuplicateUser(email, phone, excludeUserId)
    }

    suspend fun isPasswordReused(userId: Int, newHash: String): Boolean {
        val lastHashes = userRolesRepository.getLast3PasswordHashes(userId)
        return lastHashes.contains(newHash)
    }

    suspend fun isPINReused(userId: Int, newHash: String): Boolean {
        val lastHashes = userRolesRepository.getLast3PINHashes(userId)
        return lastHashes.contains(newHash)
    }

    suspend fun isPasswordRotationDue(userId: Int): Boolean {
        val lastChange = userRolesRepository.getLastPasswordChangeDate(userId) ?: return true
        val ninetyDaysInMillis = 90L * 24 * 60 * 60 * 1000
        return System.currentTimeMillis() - lastChange > ninetyDaysInMillis
    }

    suspend fun isPINRotationDue(userId: Int): Boolean {
        val lastChange = userRolesRepository.getLastPINChangeDate(userId) ?: return true
        val ninetyDaysInMillis = 90L * 24 * 60 * 60 * 1000
        return System.currentTimeMillis() - lastChange > ninetyDaysInMillis
    }

    suspend fun changeUserPassword(userId: Int, newPassword: String) {
        val hash = Converters.hashPassword(newPassword)
        userRolesRepository.updateUserPassword(userId, hash)
    }

    suspend fun changeUserPIN(userId: Int, newPIN: String) {
        val hash = Converters.hashPin(newPIN)
        userRolesRepository.updateUserPIN(userId, hash)

    }

    suspend fun checkUserPIN(userId: Int, pin: String): Boolean {
        val hashPIN = Converters.hashPin(pin)
        return userRolesRepository.checkUserPIN(userId, hashPIN)
    }

    suspend fun getUserByEmail(email: String): Users? {

        return userRolesRepository.getUserByEmail(email)
    }

    suspend fun getUserByPhone(phone: String): Users? {
        return userRolesRepository.getUserByPhone(phone)
    }

    suspend fun createUserAndReturnId(user: Users): Int {
        return userRolesRepository.createUserAndReturnId(user)
    }

    suspend fun addUserRoles(userRoles: List<UserRoles>) {
        userRolesRepository.addUserRoles(userRoles)
    }

    suspend fun getPendingModeratorRequests(): List<PendingModeratorRequest> {
        return userRolesRepository.getPendingModeratorRequests()
    }

    suspend fun approveModeratorAndGroup(userId: Int, roleId: Int, groupId: Int) {
        return userRolesRepository.approveModeratorAndGroup(userId, roleId, groupId)
    }

    suspend fun rejectModeratorAndGroup(userId: Int, roleId: Int, groupId: Int) {
        return userRolesRepository.rejectModeratorAndGroup(userId, roleId, groupId)
    }

    suspend fun getUserWithGroup(groupId: Int): Flow<List<UserWithGroup>> {
        return userRolesRepository.getUserWithGroup(groupId)
    }

    suspend fun getAllUsersWithGroup(): Flow<List<UserWithGroup>> {
        return userRolesRepository.getAllUsersWithGroup()
    }


    suspend fun fetchUsersWithGroup(
        isAdmin: Boolean,
        moderatorGroup: Int
    ): Flow<List<UserWithGroup>> {
        return if (isAdmin)
            getAllUsersWithGroup()
        else
            getUserWithGroup(moderatorGroup)
    }
}