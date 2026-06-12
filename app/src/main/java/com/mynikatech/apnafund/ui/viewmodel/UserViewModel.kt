package com.mynikatech.apnafund.ui.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mynikatech.apnafund.ApnaFundApplication
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.mappers.toDto
import com.mynikatech.apnafund.data.mappers.toEntity
import com.mynikatech.apnafund.data.model.Groups
import com.mynikatech.apnafund.data.model.UserWithGroup
import com.mynikatech.apnafund.data.model.Users
import com.mynikatech.apnafund.net.ApiException
import com.mynikatech.apnafund.net.dto.AppUsageLogDto
import com.mynikatech.apnafund.net.dto.FirebaseTokenResp
import com.mynikatech.apnafund.net.dto.GroupsWithModeratorDto
import com.mynikatech.apnafund.net.dto.LoginUserResponse
import com.mynikatech.apnafund.net.dto.ModeratorRegistrationResponse
import com.mynikatech.apnafund.net.dto.RegisterModeratorRequest
import com.mynikatech.apnafund.net.dto.RegisterOrUpdateUserRequest
import com.mynikatech.apnafund.net.dto.SaveOrUpdateUserResponse
import com.mynikatech.apnafund.net.dto.SendEmailVerificationResp
import com.mynikatech.apnafund.net.dto.SendOtpResp
import com.mynikatech.apnafund.net.dto.UserSaveSource
import com.mynikatech.apnafund.net.dto.UserStatusResponse
import com.mynikatech.apnafund.net.dto.UsersDto
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.util.Converters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {

    private val userRolesRepository = ApnaFundApplication.userRolesRepository

    val userStatus = MutableLiveData<UserStatusResponse>()
    val loading = MutableLiveData<Boolean>()
    val userLiveData = MutableLiveData<UsersDto?>()
    val refreshTrigger = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 1
    )
    val errorLiveData = MutableLiveData<ApiException>()

    init {
        refreshTrigger.tryEmit(Unit)
    }

    /** Flow source for the table (Room) */
    fun usersFlow(isAdmin: Boolean, moderatorGroupId: Int): Flow<List<UserWithGroup>> =

        refreshTrigger.flatMapLatest {
            if (isAdmin)
                userRolesRepository.getAllUsersWithGroup()
            else
                userRolesRepository.getUserWithGroup(moderatorGroupId)
        }

    /** Pull latest users from the server and cache into Room */
    suspend fun refreshUsersAndCache() {
        userRolesRepository.refreshUsers()
    }


    fun fetchUsers(): Flow<List<Users>> {
        val users = userRolesRepository.fetchAllUsers()
        return users
    }

    fun fetchUsers(isAdmin: Boolean, userId: Int): Flow<List<Users>> {
        return if (isAdmin) {
            userRolesRepository.fetchAllUsers()
        } else {
            userRolesRepository.getUsersForModerator(userId)
        }
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
        userSaveSource: UserSaveSource,
        groupRole: String? = null
    ): Result<SaveOrUpdateUserResponse> {

        val regUpdReq = RegisterOrUpdateUserRequest(
            user = user.toDto(),
            source = userSaveSource,
            roleCode = ApnaBankConstants.ROLE_MEMBER,
            groupId = groupId,
            requestorId = SessionManager.userId,
            groupRole = groupRole
        )

        return userRolesRepository.registerOrUpdateUser(regUpdReq)
    }

    suspend fun isDuplicate(email: String, phone: String, excludeUserId: Int = 0): Boolean {
        return userRolesRepository.isDuplicateUser(email, phone, excludeUserId)
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

    suspend fun getUserByPhone(phone: String): LoginUserResponse? {
        return userRolesRepository.getUserByPhone(phone)
    }

    fun fetchUserByEmailandPhone(email: String, phone: String) {
        viewModelScope.launch {
            try {
                val response = userRolesRepository.getUserByPhoneAndEmail(email, phone)
                userLiveData.value = response?.user
            } catch (e: ApiException) {
                errorLiveData.value = e
            }
        }
    }
    fun fetchUserByPhoneAndGroupCode(phone: String, groupCode: String ) {
        viewModelScope.launch {
            try {
                val response =
                    userRolesRepository.getUserByPhoneAndGroupCode(
                        phone,
                        groupCode
                    )

                userLiveData.value = response?.user

            } catch (e: ApiException) {
                errorLiveData.value = e
            }
        }
    }

    fun getUserWithGroup(groupId: Int): Flow<List<UserWithGroup>> {
        return userRolesRepository.getUserWithGroup(groupId)
    }

    fun getAllUsersWithGroup(): Flow<List<UserWithGroup>> {
        return userRolesRepository.getAllUsersWithGroup()
    }


    fun fetchUsersWithGroup(
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

    suspend fun sendOtp(
        userId: Int,
        phone: String,
        purpose: String
    ): SendOtpResp {
        return userRolesRepository.sendOtp(
            userId = userId,
            phoneNumber = phone,
            purpose = purpose
        )
    }

    suspend fun verifyOtp(
        otp: String,
        userId: Int,
        purpose: String,
        channel: String
    ): Boolean {

        return userRolesRepository.verifyOtp(
            otp = otp,
            userId = userId,
            purpose = purpose,
            channel = channel
        )
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

    fun checkUserStatus(email: String) {
        viewModelScope.launch {
            loading.value = true
            try {
                //val user = userRolesRepository.getUserByEmail(email)
                val result = userRolesRepository.checkUserStatus(email)
                userStatus.value = result
            } catch (e: Exception) {
                // handle error (network etc.)
            } finally {
                loading.value = false
            }
        }
    }

    fun fetchUserByEmail(email: String) {
        viewModelScope.launch {
            try {
                val response = userRolesRepository.getUserByEmail(email)
                userLiveData.value = response?.user
            } catch (e: ApiException) {
                errorLiveData.value = e
            }
        }
    }

    suspend fun findExistingUserByEmail(
        email: String
    ): LoginUserResponse? {

        return try {

            userRolesRepository.getUserByEmail(email)

        } catch (e: ApiException) {

            null
        }
    }

    suspend fun findExistingUserByPhone(
        phone: String
    ): LoginUserResponse? {

        return try {

            userRolesRepository.getUserByPhone(phone)

        } catch (e: ApiException) {

            null
        }
    }

    fun clearUser() {
        userLiveData.value = null
    }

    fun getGroupsForModeratorUser(userId: Int): Flow<List<Groups>> =
        flow {
            emit(userRolesRepository.getGroupsForModeratorUser(userId))
        }
            .map { dtos -> dtos.map { it.toEntity() } }
            .catch { e ->
                Log.e("UserViewModel", "Error fetching moderator groups", e)
                emit(emptyList())
            }

    fun getGroupsForModeratorUserWithModInfo(userId: Int): Flow<List<GroupsWithModeratorDto>> =
        flow {
            emit(userRolesRepository.getGroupsForModeratorUserWithModInfo(userId))
        }
            .catch { e ->
                Log.e("UserViewModel", "Error fetching moderator groups", e)
                emit(emptyList())
            }

    fun addUsageLog(
        userId: Int,
        eventType: String,
        screenName: String? = null,
        details: String? = null
    ) {

        viewModelScope.launch {

            try {

                userRolesRepository.addUsageLog(
                    AppUsageLogDto(
                        userId = userId,
                        eventType = eventType,
                        screenName = screenName,
                        details = details
                    )
                )

            } catch (_: Exception) {
            }
        }
    }
}