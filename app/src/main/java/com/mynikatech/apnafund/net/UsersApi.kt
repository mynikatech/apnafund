package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.net.dto.FundWithDetailsDto
import com.mynikatech.apnafund.net.dto.FundsDto
import com.mynikatech.apnafund.net.dto.GroupMembersDto
import com.mynikatech.apnafund.net.dto.GroupsDto
import com.mynikatech.apnafund.net.dto.UserDetailsDto
import com.mynikatech.apnafund.net.dto.UserFundDetailsDto
import com.mynikatech.apnafund.net.dto.UserNotificationsDto
import com.mynikatech.apnafund.net.dto.UsersDto
import com.mynikatech.apnafund.net.dto.UserProfileDto
import com.mynikatech.apnafund.net.dto.UserWithGroupDto

interface UsersApi {
    // CRUD
    suspend fun getUsers(): List<UsersDto>
    suspend fun getUser(id: Int): UsersDto?
    suspend fun addUser(user: UsersDto): Int
    suspend fun updateUser(id: Int, user: UsersDto): Boolean
    suspend fun deleteUser(id: Int): Boolean
    suspend fun deleteAll(): Boolean

    // lookups/checks
    suspend fun doesUserExists(emailId: String): Boolean
    suspend fun countMatchingUsers(email: String, phone: String, excludeUserId: Int): Int
    suspend fun getUserByEmail(email: String): UsersDto?
    suspend fun getUserByPhone(phone: String): UsersDto?
    suspend fun validateUser(email: String, password: String): UsersDto?
    suspend fun checkUserPIN(userId: Int, pin: String): Boolean
    suspend fun doesGroupHasModerator(groupId: Int): Boolean

    // with group
    suspend fun getUserWithGroup(groupId: Int? = null): List<UserWithGroupDto>
    suspend fun getAllUsersWithGroup(): List<UserWithGroupDto>
    suspend fun getGroupMember(userId: Int, groupId: Int): GroupMembersDto?
    suspend fun getUserProfile(userId: Int): List<UserProfileDto>?
    suspend fun updateUserPassword(id: Int, passwordHash: String): Boolean
    suspend fun updateUserPIN(id: Int, pinHash: String): Boolean
    suspend fun getGroupForUser(userId: Int): GroupsDto?
    suspend fun getFundsForUser(userId: Int): List<FundsDto>
    suspend fun getTotalDeposit(userId: Int, fundId: Int): Double?
    suspend fun getPerMemberExpectedMaturityAmount(fundId: Int): Double?
    suspend fun getUserNotifications(userId: Int): List<UserNotificationsDto>?
    suspend fun getUserDetails(userId: Int): UserDetailsDto
    suspend fun getTotalLoanAmount(userId: Int, fundId: Int): Double
    suspend fun getFundWithDetails(fundId: Int): FundWithDetailsDto
    suspend fun getUserFundDetails(userId: Int, fundId: Int): UserFundDetailsDto?


}


