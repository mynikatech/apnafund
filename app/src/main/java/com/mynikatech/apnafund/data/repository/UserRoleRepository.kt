package com.mynikatech.apnafund.data.repository

// Removed: androidx.room.withTransaction
// Removed: AppLocator

import com.mynikatech.apnafund.Exception.InvalidSessionException
import com.mynikatech.apnafund.data.mappers.toDto
import com.mynikatech.apnafund.data.mappers.toEntity
import com.mynikatech.apnafund.data.model.GroupMembers
import com.mynikatech.apnafund.data.model.PendingModeratorRequest
import com.mynikatech.apnafund.data.model.Roles
import com.mynikatech.apnafund.data.model.UserPasswordHistory
import com.mynikatech.apnafund.data.model.UserPinHistory
import com.mynikatech.apnafund.data.model.UserProfile
import com.mynikatech.apnafund.data.model.UserRoles
import com.mynikatech.apnafund.data.model.UserWithGroup
import com.mynikatech.apnafund.data.model.Users
import com.mynikatech.apnafund.net.AdminApi
import com.mynikatech.apnafund.net.ApiException
import com.mynikatech.apnafund.net.GroupsApi
import com.mynikatech.apnafund.net.PasswordHistoryApi
import com.mynikatech.apnafund.net.PinHistoryApi
import com.mynikatech.apnafund.net.RolesApi
import com.mynikatech.apnafund.net.UserRolesApi
import com.mynikatech.apnafund.net.UsersApi
import com.mynikatech.apnafund.net.dto.FirebaseTokenResp
import com.mynikatech.apnafund.net.dto.GroupsDto
import com.mynikatech.apnafund.net.dto.LoginUserResponse
import com.mynikatech.apnafund.net.dto.ModeratorRegistrationResponse
import com.mynikatech.apnafund.net.dto.RegisterModeratorRequest
import com.mynikatech.apnafund.net.dto.RegisterOrUpdateUserRequest
import com.mynikatech.apnafund.net.dto.RolesDto
import com.mynikatech.apnafund.net.dto.SaveOrUpdateUserResponse
import com.mynikatech.apnafund.net.dto.SendEmailVerificationReq
import com.mynikatech.apnafund.net.dto.SendEmailVerificationResp
import com.mynikatech.apnafund.net.dto.UpdateFirebaseUidReq
import com.mynikatech.apnafund.net.dto.UsersDto
import com.mynikatech.apnafund.net.dto.VerifyEmailReq
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Suppress("unused") // we kept some method names for API compatibility
class UserRoleRepository(
    // Keep constructor parameters for DI compatibility, but we no longer use local DB/DAOs.
    // (Commented out property storage to avoid accidental usage.)
    /* userDao: UsersDao,
    rolesDao: RolesDao,
    userRolesDao: UserRolesDao,
    passwordHistDao: PasswordHistoryDao,
    adminDao: AdminDao,
    pinHistoryDao: PinHistoryDao, */

    private val usersApi: UsersApi,
    private val rolesApi: RolesApi,
    private val groupsApi: GroupsApi,
    private val userRolesApi: UserRolesApi,
    private val pinHistoryApi: PinHistoryApi,
    private val passwordHistoryApi: PasswordHistoryApi,
    private val adminApi: AdminApi
) {

    // --- Sync helpers (no local cache) ---

    suspend fun refreshUsers() = withContext(Dispatchers.IO) {
        // Network-only: Fetch to validate connectivity; no local writes.
        runCatching { usersApi.getUsers() }.getOrNull()
    }

    suspend fun refreshOne(id: Int) {
        // Network-only no-op: just hit server if you want to ensure reachability.
        runCatching { usersApi.getUser(id) }.getOrNull()
    }

    suspend fun createUserRemoteAndCache(user: Users): Int = withContext(Dispatchers.IO) {
        // Network-only: create and return server id
        usersApi.addUser(user.toDto())
    }

    suspend fun updateUserRemote(user: Users): Boolean = withContext(Dispatchers.IO) {
        val id = requireNotNull(user.userId.takeIf { it != 0 }) { "userId required" }
        usersApi.updateUser(id, user.toDto())
    }

    suspend fun deleteUserRemoteAndCache(userId: Int): Boolean = withContext(Dispatchers.IO) {
        usersApi.deleteUser(userId)
    }

    // --- Roles & memberships (network-only) ---

    suspend fun ensureRoleRemoteAndCache(userId: Int, roleCode: String) =
        withContext(Dispatchers.IO) {
            val existingRemote =
                runCatching { userRolesApi.getAllUserRoles(userId) }.getOrDefault(emptyList())
            if (existingRemote.any { it.equals(roleCode, ignoreCase = true) }) return@withContext

            val roleId = rolesApi.getRoleIdByRoleCode(roleCode)
            rolesApi.assignRole(userId, roleId)
        }

    suspend fun ensureGroupMemberRemoteAndCache(member: GroupMembers) =
        withContext(Dispatchers.IO) {
            val already = usersApi.getGroupMember(member.userId, member.groupId)
            if (already == null) {
                groupsApi.addGroupMember(member)
            }
        }

    suspend fun ensureModeratorIfNoneRemoteAndCache(userId: Int, groupId: Int) =
        withContext(Dispatchers.IO) {
            val hasModerator =
                runCatching { usersApi.doesGroupHasModerator(groupId) }.getOrDefault(false)
            if (!hasModerator) ensureRoleRemoteAndCache(userId, "GROUP_MODERATOR")
        }

    suspend fun createUser(user: Users) {
        usersApi.addUser(user.toDto())
    }

    suspend fun createRole(role: Roles) {
        rolesApi.addRole(role.toDto())
    }

    suspend fun deleteUser(id: Int) {
        usersApi.deleteUser(id)
    }

    suspend fun updateUser(id: Int, user: Users) {
        usersApi.updateUser(id, user.toDto())
    }

    fun fetchAllUsers(): Flow<List<Users>> = flow {
        val dtos: List<UsersDto> = usersApi.getUsers()
        emit(dtos.map { it.toEntity() })
    }.catch { emit(emptyList()) }
        .flowOn(Dispatchers.IO)

    suspend fun isDuplicateUser(email: String, phone: String, excludeUserId: Int = 0): Boolean {
        return usersApi.countMatchingUsers(email, phone, excludeUserId) > 0
    }

    suspend fun fetchUser(userId: Int): Users? {
        return usersApi.getUser(userId)?.toEntity()
    }

    suspend fun doesUserExists(emailId: String): Boolean {
        return usersApi.doesUserExists(emailId)
    }

    suspend fun addUserRoles(userRoles: List<UserRoles>) {
        userRolesApi.addUserRoles(userRoles.toDto())
        // no local persist
    }

    suspend fun getPrivilegesGroupedByRole(): Map<Int, List<String>> {
        return rolesApi.getAllRolesWithPrivileges()
            .groupBy { it.roleId }
            .mapValues { entry -> entry.value.map { it.privilegeCode } }
    }

    suspend fun getRoleCodesByRoleId(): Map<String, Int> {
        val roles: List<RolesDto> = rolesApi.getAllRoles()
        return roles.mapNotNull { dto ->
            val code = dto.roleCode
            val id = when (val rid = dto.roleId) {
                null -> return@mapNotNull null
                is Int -> rid
                is Long -> rid.toInt()
                else -> return@mapNotNull null
            }
            code to id
        }.toMap()
    }

    suspend fun getRolebyRoleCode(roleCode: String): Int {
        return rolesApi.getRoleIdByRoleCode(roleCode)
    }

    suspend fun getAllUserRoles(userId: Int): List<String> {
        return userRolesApi.getAllUserRoles(userId)
    }

    suspend fun getAllUserRoleIds(userId: Int): List<Int> {
        return userRolesApi.getAllUserRoleIds(userId)
    }

    suspend fun getUserProfile(userId: Int): UserProfile =
        withContext(Dispatchers.IO) {

            val remote = usersApi.getUserProfile(userId)

            if (remote == null) {
                throw InvalidSessionException("User profile not found for userId=$userId")
            }

            remote.toEntity()
        }

    suspend fun insertPasswordHistory(entry: UserPasswordHistory) {
        passwordHistoryApi.insertPasswordHistory(entry)
    }

    suspend fun insertPINHistory(entry: UserPinHistory) {
        pinHistoryApi.insertPINHistory(entry)
    }

    suspend fun getLast3PasswordHashes(userId: Int): List<String> {
        return passwordHistoryApi.getLast3PasswordHashes(userId)
    }

    suspend fun validateUserPasswordChange(userId: Int, password: String) {
        return usersApi.validateUserPasswordChange(userId, password)
    }

    suspend fun getLast3PINHashes(userId: Int): List<String> {
        return pinHistoryApi.getLast3PINHashes(userId)
    }

    suspend fun getLastPasswordChangeDate(userId: Int): Long? {
        return passwordHistoryApi.getLastPasswordChangeDate(userId)
    }

    suspend fun getLastPINChangeDate(userId: Int): Long? {
        return pinHistoryApi.getLastPINChangeDate(userId)
    }

    suspend fun getUserByEmail(email: String): LoginUserResponse? {
        return usersApi.getUserByEmail(email)
    }

    suspend fun getUserByPhone(phone: String): LoginUserResponse? {
        return usersApi.getUserByPhone(phone)
    }

    suspend fun checkUserPIN(userId: Int, pin: String): Boolean {
        return usersApi.checkUserPIN(userId, pin)
    }

    suspend fun createUserAndReturnId(user: Users): Int {
        // Previously local DB insert; now create on server and return new id.
        return usersApi.addUser(user.toDto())
    }

    suspend fun getAllRoles(): List<Roles> {
        return rolesApi.getAllRoles().toEntity()
    }

    suspend fun getPendingModeratorRequests(): List<PendingModeratorRequest> {
        return userRolesApi.getPendingModeratorRequests().toEntity()
    }

    suspend fun approveModeratorAndGroup(
        userId: Int,
        roleId: Int,
        groupId: Int,
        moderatorName: String,
        moderatorEmail: String,
        groupName: String
    ) {
        adminApi.approveModeratorAndGroup(
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
        adminApi.rejectModeratorAndGroup(
            userId,
            roleId,
            groupId,
            moderatorName,
            moderatorEmail,
            groupName
        )
    }

    fun getUserWithGroup(groupId: Int): Flow<List<UserWithGroup>> =
        flow {
            val dtos = usersApi.getUserWithGroup(groupId)
            emit(dtos.map { it.toEntity() })
        }.catch { emit(emptyList()) }
            .flowOn(Dispatchers.IO)

    fun getAllUsersWithGroup(): Flow<List<UserWithGroup>> =
        flow {
            val dtos = usersApi.getAllUsersWithGroup()
            emit(dtos.map { it.toEntity() })
        }.catch { emit(emptyList()) }
            .flowOn(Dispatchers.IO)

    suspend fun createGroupMember(groupMembers: GroupMembers) {
        groupsApi.addGroupMember(groupMembers)
    }

    suspend fun updateUserPassword(userId: Int, newPassword: String) =
        withContext(Dispatchers.IO) {
            // Remote only; server should handle history.
            usersApi.updateUserPassword(userId, newPassword)
        }

    suspend fun updateUserPIN(userId: Int, pinHash: String) =
        withContext(Dispatchers.IO) {
            val ok = usersApi.updateUserPIN(userId, pinHash)
            if (!ok) error("Server rejected PIN update (userId=$userId)")
        }

    // One-shot list (network only)
    suspend fun loadUsers(forceRefresh: Boolean = true): List<Users> {
        val dtos = usersApi.getUsers()
        return dtos.map { it.toEntity() }
    }

    // One-shot details (network only)
    suspend fun loadUser(id: Int, forceRefresh: Boolean = true): Users? {
        return usersApi.getUser(id)?.toEntity()
    }

    suspend fun registerModeratorAndGroup(
        regModReq: RegisterModeratorRequest
    ): Result<ModeratorRegistrationResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = usersApi.registerModeratorAndGroup(regModReq)
            Result.success(resp)
        } catch (e: ApiException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerOrUpdateUser(
        regUpdateReq: RegisterOrUpdateUserRequest
    ): Result<SaveOrUpdateUserResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = usersApi.registerOrUpdateUser(regUpdateReq)
                Result.success(response)
            } catch (e: ApiException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun verifyEmailOtp(otp: String, userId: Int, purpose: String): Boolean {
        return usersApi.verifyEmailOtp(
            VerifyEmailReq(token = otp, userId = userId, purpose = purpose)
        )
    }

    suspend fun resendEmailVerification(
        userId: Int,
        email: String,
        userName: String,
        purpose: String
    ): SendEmailVerificationResp {
        return usersApi.sendEmailVerification(
            SendEmailVerificationReq(
                userId = userId,
                emailId = email,
                userName = userName,
                purpose = purpose
            )
        )
    }

    suspend fun isEmailVerified(userId: Int): Boolean {
        return usersApi.isEmailVerified(userId)
    }

    suspend fun updateFirebaseUserId(userId: Int, firebaseUid: String) {
        usersApi.updateFirebaseUserId(
            UpdateFirebaseUidReq(
                userId = userId,
                firebaseUid = firebaseUid
            )
        )
    }

    suspend fun getFirebaseTokenForUser(userId: Int): FirebaseTokenResp {
        return usersApi.getFirebaseTokenForUser(userId)
    }

    suspend fun getGroupsForUser(userId: Int): List<GroupsDto> {
        return usersApi.getGroupsForUser(userId)
    }

    suspend fun getGroupsForModeratorUser(userId: Int): List<GroupsDto> {
        return usersApi.getGroupsForModeratorUser(userId)
    }
}
