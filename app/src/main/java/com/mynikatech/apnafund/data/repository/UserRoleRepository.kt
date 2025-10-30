package com.mynikatech.apnafund.data.repository

// Removed: androidx.room.withTransaction
// Removed: AppLocator

import com.mynikatech.apnafund.data.model.GroupMembers
import com.mynikatech.apnafund.data.model.PendingModeratorRequest
import com.mynikatech.apnafund.data.model.Roles
import com.mynikatech.apnafund.data.model.UserPasswordHistory
import com.mynikatech.apnafund.data.model.UserPinHistory
import com.mynikatech.apnafund.data.model.UserProfile
import com.mynikatech.apnafund.data.model.UserRoles
import com.mynikatech.apnafund.data.model.UserWithGroup
import com.mynikatech.apnafund.data.model.Users
import com.mynikatech.apnafund.data.mappers.toDto
import com.mynikatech.apnafund.data.mappers.toEntity
import com.mynikatech.apnafund.net.AdminApi
import com.mynikatech.apnafund.net.GroupsApi
import com.mynikatech.apnafund.net.PasswordHistoryApi
import com.mynikatech.apnafund.net.PinHistoryApi
import com.mynikatech.apnafund.net.RolesApi
import com.mynikatech.apnafund.net.UserRolesApi
import com.mynikatech.apnafund.net.UsersApi
import com.mynikatech.apnafund.net.dto.RolesDto
import com.mynikatech.apnafund.net.dto.UserProfileDto
import com.mynikatech.apnafund.net.dto.UsersDto
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

    suspend fun updateUserRemoteAndCache(user: Users): Boolean = withContext(Dispatchers.IO) {
        val id = requireNotNull(user.userId.takeIf { it != 0 }) { "userId required" }
        usersApi.updateUser(id, user.toDto())
    }

    suspend fun deleteUserRemoteAndCache(userId: Int): Boolean = withContext(Dispatchers.IO) {
        usersApi.deleteUser(userId)
    }

    // --- Roles & memberships (network-only) ---

    suspend fun ensureRoleRemoteAndCache(userId: Int, roleCode: String) = withContext(Dispatchers.IO) {
        val existingRemote = runCatching { userRolesApi.getAllUserRoles(userId) }.getOrDefault(emptyList())
        if (existingRemote.any { it.equals(roleCode, ignoreCase = true) }) return@withContext

        val roleId = rolesApi.getRoleIdByRoleCode(roleCode)
            rolesApi.assignRole(userId, roleId)
    }

    suspend fun ensureGroupMemberRemoteAndCache(member: GroupMembers) = withContext(Dispatchers.IO) {
        val already =  usersApi.getGroupMember(member.userId, member.groupId)
        if (already == null) {
            groupsApi.addGroupMember(member)
        }
    }

    suspend fun ensureModeratorIfNoneRemoteAndCache(userId: Int, groupId: Int) = withContext(Dispatchers.IO) {
        val hasModerator = runCatching { usersApi.doesGroupHasModerator(groupId) }.getOrDefault(false)
        if (!hasModerator) ensureRoleRemoteAndCache(userId, "MODERATOR")
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
            val code = dto.roleCode ?: return@mapNotNull null
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

    suspend fun getUserProfile(userId: Int): List<UserProfile> = withContext(Dispatchers.IO) {
        val remote: List<UserProfileDto> = usersApi.getUserProfile(userId).orEmpty()
        remote.map { it.toEntity() } // network only
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

    suspend fun getLast3PINHashes(userId: Int): List<String> {
        return pinHistoryApi.getLast3PINHashes(userId)
    }

    suspend fun getLastPasswordChangeDate(userId: Int): Long? {
        return passwordHistoryApi.getLastPasswordChangeDate(userId)
    }

    suspend fun getLastPINChangeDate(userId: Int): Long? {
        return pinHistoryApi.getLastPINChangeDate(userId)
    }

    suspend fun getUserByEmail(email: String): Users? {
        return usersApi.getUserByEmail(email)?.toEntity()
    }

    suspend fun getUserByPhone(phone: String): Users? {
        return usersApi.getUserByPhone(phone)?.toEntity()
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

    suspend fun approveModeratorAndGroup(userId: Int, roleId: Int, groupId: Int) {
        adminApi.approveModeratorAndGroup(userId, roleId, groupId)
    }

    suspend fun rejectModeratorAndGroup(userId: Int, roleId: Int, groupId: Int) {
        adminApi.rejectModeratorAndGroup(userId, roleId, groupId)
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

    suspend fun updateUserPassword(userId: Int, passwordHash: String) =
        withContext(Dispatchers.IO) {
            // Remote only; server should handle history.
            val ok = usersApi.updateUserPassword(userId, passwordHash)
            if (!ok) error("Server rejected password update (userId=$userId)")
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
}
