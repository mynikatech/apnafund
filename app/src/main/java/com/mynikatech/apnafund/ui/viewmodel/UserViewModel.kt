package com.mynikatech.apnafund.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.mynikatech.apnafund.ApnaFundApplication
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.mappers.toDto
import com.mynikatech.apnafund.data.model.PendingModeratorRequest
import com.mynikatech.apnafund.data.model.UserRoles
import com.mynikatech.apnafund.data.model.UserWithGroup
import com.mynikatech.apnafund.data.model.Users
import com.mynikatech.apnafund.net.ApiException
import com.mynikatech.apnafund.net.dto.FirebaseTokenResp
import com.mynikatech.apnafund.net.dto.LoginUserResponse
import com.mynikatech.apnafund.net.dto.ModeratorRegistrationResponse
import com.mynikatech.apnafund.net.dto.RegisterModeratorRequest
import com.mynikatech.apnafund.net.dto.RegisterOrUpdateUserRequest
import com.mynikatech.apnafund.net.dto.SaveOrUpdateUserResponse
import com.mynikatech.apnafund.net.dto.SendEmailVerificationResp
import com.mynikatech.apnafund.net.dto.UserSaveSource
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

    suspend fun saveOrUpdateUser(
        user: Users,
        groupId: Int = 0,
        userSaveSource: UserSaveSource
    ): Result<SaveOrUpdateUserResponse> {

        val regUpdReq = RegisterOrUpdateUserRequest(
            user = user.toDto(),
            source = userSaveSource,
            roleCode = ApnaBankConstants.ROLE_MEMBER,
            groupId = groupId
        )

        return userRolesRepository.registerOrUpdateUser(regUpdReq)
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
        userRolesRepository.updateUserPassword(userId, newPassword)
    }

    suspend fun changeUserPIN(userId: Int, newPIN: String) {
        val hash = Converters.hashPin(newPIN)
        userRolesRepository.updateUserPIN(userId, hash)

    }

    suspend fun checkUserPIN(userId: Int, pin: String): Boolean {
        val hashPIN = Converters.hashPin(pin)
        return userRolesRepository.checkUserPIN(userId, hashPIN)
    }

    suspend fun getUserByEmail(email: String): LoginUserResponse? {

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

    suspend fun approveModeratorAndGroup(
        userId: Int,
        roleId: Int,
        groupId: Int,
        moderatorName: String,
        moderatorEmail: String,
        groupName: String
    ) {
        return userRolesRepository.approveModeratorAndGroup(
            userId,
            roleId,
            groupId,
            moderatorName,
            moderatorEmail,
            groupName
        )
    }

    suspend fun rejectModeratorAndGroup(
        userId: Int,
        roleId: Int,
        groupId: Int,
        moderatorName: String,
        moderatorEmail: String,
        groupName: String
    ) {
        return userRolesRepository.rejectModeratorAndGroup(
            userId,
            roleId,
            groupId,
            moderatorName,
            moderatorEmail,
            groupName
        )
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

    suspend fun registerModeratorAndGroup(
        regModReq: RegisterModeratorRequest
    ): Result<ModeratorRegistrationResponse> {

        return userRolesRepository.registerModeratorAndGroup(regModReq)
    }

    suspend fun registerOrUpdateUser(regUpdateReq: RegisterOrUpdateUserRequest) {
        userRolesRepository.registerOrUpdateUser(regUpdateReq)
    }

    suspend fun verifyEmailOtp(otp: String, userId: Int, purpose: String): Boolean {
        return userRolesRepository.verifyEmailOtp(otp, userId, purpose)
    }

    suspend fun resendEmailVerification(
        userId: Int,
        email: String,
        userName: String,
        purpose: String
    ): SendEmailVerificationResp {
        return userRolesRepository.resendEmailVerification(userId, email, userName, purpose)
    }

    suspend fun isEmailVerified(userId: Int): Boolean {
        return userRolesRepository.isEmailVerified(userId)
    }

    suspend fun updateFirebaseUserIdSafely(userId: Int, firebaseUid: String): Boolean {
        return try {
            userRolesRepository.updateFirebaseUserId(userId, firebaseUid)
            true
        } catch (e: ApiException) {
            Log.e("AUTH", "Failed to update firebase uid", e)
            false
        }
    }

    suspend fun getFirebaseTokenForUser(userId: Int): FirebaseTokenResp {
        return userRolesRepository.getFirebaseTokenForUser(userId)
    }
}