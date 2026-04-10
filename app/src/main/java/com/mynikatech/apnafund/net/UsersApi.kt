package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.net.dto.FirebaseTokenResp
import com.mynikatech.apnafund.net.dto.FundWithDetailsDto
import com.mynikatech.apnafund.net.dto.FundsDto
import com.mynikatech.apnafund.net.dto.GroupMembersDto
import com.mynikatech.apnafund.net.dto.GroupsDto
import com.mynikatech.apnafund.net.dto.LoginUserResponse
import com.mynikatech.apnafund.net.dto.ModeratorRegistrationResponse
import com.mynikatech.apnafund.net.dto.RegisterModeratorRequest
import com.mynikatech.apnafund.net.dto.RegisterOrUpdateUserRequest
import com.mynikatech.apnafund.net.dto.SaveOrUpdateUserResponse
import com.mynikatech.apnafund.net.dto.SendEmailVerificationReq
import com.mynikatech.apnafund.net.dto.SendEmailVerificationResp
import com.mynikatech.apnafund.net.dto.UpdateFirebaseUidReq
import com.mynikatech.apnafund.net.dto.UserDetailsDto
import com.mynikatech.apnafund.net.dto.UserFundDetailsDto
import com.mynikatech.apnafund.net.dto.UserNotificationsDto
import com.mynikatech.apnafund.net.dto.UsersDto
import com.mynikatech.apnafund.net.dto.UserProfileDto
import com.mynikatech.apnafund.net.dto.UserWithGroupDto
import com.mynikatech.apnafund.net.dto.VerifyEmailReq

interface UsersApi {
    // CRUD
    suspend fun getUsers(): List<UsersDto>
    suspend fun getUser(id: Int): UsersDto?
    suspend fun addUser(user: UsersDto): Int
    suspend fun updateUser(id: Int, user: UsersDto): Boolean
    suspend fun deleteUser(id: Int): Boolean
    suspend fun deleteAll(): Boolean
    suspend fun getUsersForModerator(userId: Int): List<UsersDto>

    // lookups/checks
    suspend fun doesUserExists(emailId: String): Boolean
    suspend fun countMatchingUsers(email: String, phone: String, excludeUserId: Int): Int
    suspend fun getUserByEmail(email: String): LoginUserResponse?
    suspend fun getUserByPhone(phone: String): LoginUserResponse?
    suspend fun validateUser(email: String, password: String): UsersDto?
    suspend fun checkUserPIN(userId: Int, pin: String): Boolean
    suspend fun doesGroupHasModerator(groupId: Int): Boolean
    suspend fun validateUserPasswordChange(userId: Int, password: String)

    // with group
    suspend fun getUserWithGroup(groupId: Int? = null): List<UserWithGroupDto>
    suspend fun getAllUsersWithGroup(): List<UserWithGroupDto>
    suspend fun getGroupMember(userId: Int, groupId: Int): GroupMembersDto?
    suspend fun getUserProfile(userId: Int): UserProfileDto?
    suspend fun updateUserPassword(id: Int, passwordHash: String)
    suspend fun updateUserPIN(id: Int, pinHash: String): Boolean
    suspend fun getGroupForUser(userId: Int): GroupsDto?
    suspend fun getGroupsForUser(userId: Int): List<GroupsDto>
    suspend fun getGroupsForModeratorUser(userId: Int): List<GroupsDto>
    suspend fun getFundsForUser(userId: Int): List<FundsDto>
    suspend fun getTotalDeposit(userId: Int, fundId: Int): Double?
    suspend fun getPerMemberExpectedMaturityAmount(fundId: Int): Double?
    suspend fun getUserNotifications(userId: Int): List<UserNotificationsDto>?
    suspend fun getUserDetails(userId: Int, groupId: Int?): UserDetailsDto
    suspend fun getTotalLoanAmount(userId: Int, fundId: Int): Double
    suspend fun getFundWithDetails(fundId: Int): FundWithDetailsDto
    suspend fun getUserFundDetails(userId: Int, fundId: Int): UserFundDetailsDto?
    suspend fun registerModeratorAndGroup(regModReq: RegisterModeratorRequest): ModeratorRegistrationResponse
    suspend fun registerOrUpdateUser(registerOrUpdateUserRequest: RegisterOrUpdateUserRequest): SaveOrUpdateUserResponse
    suspend fun verifyEmailOtp(req: VerifyEmailReq): Boolean
    suspend fun sendEmailVerification(req: SendEmailVerificationReq): SendEmailVerificationResp
    suspend fun isEmailVerified(userId: Int): Boolean
    suspend fun updateFirebaseUserId( req: UpdateFirebaseUidReq): Boolean
    suspend fun getFirebaseTokenForUser(userId: Int): FirebaseTokenResp
    suspend fun getUnreadNotificationCount(userId: Int): Int
    suspend fun markNotificationRead(notificationId: Int): Boolean
}


