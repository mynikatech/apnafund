package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.Feedback
import com.mynikatech.apnafund.data.model.FeedbackWithUserGroup
import com.mynikatech.apnafund.data.model.UserDetails
import com.mynikatech.apnafund.data.model.UserFundDetails
import com.mynikatech.apnafund.data.model.UserLoanDetails
import com.mynikatech.apnafund.data.model.UserPasswordHistory
import com.mynikatech.apnafund.data.model.UserPinHistory
import com.mynikatech.apnafund.data.model.UserWithGroup
import com.mynikatech.apnafund.data.model.Users
import com.mynikatech.apnafund.net.dto.FeedbackDto
import com.mynikatech.apnafund.net.dto.FeedbackWithUserGroupDto
import com.mynikatech.apnafund.net.dto.UserDetailsDto
import com.mynikatech.apnafund.net.dto.UserFundDetailsDto
import com.mynikatech.apnafund.net.dto.UserLoanDetailsDto
import com.mynikatech.apnafund.net.dto.UserPasswordHistoryDto
import com.mynikatech.apnafund.net.dto.UserPinHistoryDto
import com.mynikatech.apnafund.net.dto.UserWithGroupDto
import com.mynikatech.apnafund.net.dto.UsersDto

// Room → DTO
fun Users.toDto(): UsersDto = UsersDto(
    userId = if (userId == 0) null else userId,
    firstName = firstName,
    lastName = lastName,
    emailId = emailId ?: "",
    phoneNumber = phoneNumber,
    status = status,
    userCode = userCode,
    isPinSet = isPinSet,
    firebaseUserId = firebaseUserId,
    createdDate = createdDate,
    passwordHash = passwordHash,
    emailVerified = emailVerified,
    emailVerifiedAt = emailVerifiedAt,
    hashPIN = hashPIN

)

// DTO → Room
fun UsersDto.toEntity(): Users = Users(
    userId = userId ?: 0,
    firstName = firstName,
    lastName = lastName,
    emailId = emailId,
    phoneNumber = phoneNumber,
    status = status,
    passwordHash = passwordHash,
    createdDate = createdDate ?: "",
    isPinSet = isPinSet ?: false,
    hashPIN = hashPIN,
    firebaseUserId = firebaseUserId,
    emailVerified = emailVerified,
    emailVerifiedAt = emailVerifiedAt,
    userCode = userCode
)

fun List<UsersDto>.toEntity(): List<Users> = map { it.toEntity() }
fun List<Users>.toDto(): List<UsersDto> = map { it.toDto() }

fun UserDetails.toDto(): UserDetailsDto = UserDetailsDto(
    firstName = firstName,
    lastName = lastName,
    emailId = emailId,
    phoneNumber = phoneNumber,
    status = status,
    userCode = userCode,
    userRoles = userRoles,
    userNotifications = userNotifications.toDto(),
    userFunds = userFunds.toDto(),
    groups = groups?: emptyList()

)

// DTO → Room
fun UserDetailsDto.toEntity(): UserDetails = UserDetails(
    firstName = firstName,
    lastName = lastName,
    emailId = emailId,
    phoneNumber = phoneNumber,
    status = status,
    userCode = userCode,
    userRoles = userRoles,
    userNotifications = userNotifications.toEntity(),
    userFunds = userFunds.toEntity(),
    groups = groups

)

fun UserFundDetails.toDto(): UserFundDetailsDto = UserFundDetailsDto(
    fundDetails = fundDetails.toDto(),
    totalLoanAmount = totalLoanAmount,
    totalDeposit = totalDeposit,
    userExpMatAmount = userExpMatAmount
)

// DTO → Room
fun UserFundDetailsDto.toEntity(): UserFundDetails = UserFundDetails(
    fundDetails = fundDetails.toEntity(),
    totalLoanAmount = totalLoanAmount,
    totalDeposit = totalDeposit,
    userExpMatAmount = userExpMatAmount
)

fun UserLoanDetails.toDto(): UserLoanDetailsDto = UserLoanDetailsDto(
    totalCurrIntPaid = totalCurrIntPaid,
    totalOutstandingAmount = totalOutstandingAmount
)

// DTO → Room
fun UserLoanDetailsDto.toEntity(): UserLoanDetails = UserLoanDetails(
    totalCurrIntPaid = totalCurrIntPaid,
    totalOutstandingAmount = totalOutstandingAmount
)


fun UserPasswordHistory.toDto(): UserPasswordHistoryDto = UserPasswordHistoryDto(
    id = if (id == 0) null else id,
    userId = userId,
    passwordHash = passwordHash,
    changedAt = changedAt
)

fun UserPasswordHistoryDto.toEntity(): UserPasswordHistory = UserPasswordHistory(
    id = id ?: 0,
    userId = userId,
    passwordHash = passwordHash,
    changedAt = changedAt ?: System.currentTimeMillis()
)

/* ---------- UserPinHistory ---------- */

fun UserPinHistory.toDto(): UserPinHistoryDto = UserPinHistoryDto(
    id = if (id == 0) null else id,
    userId = userId,
    pinHash = pinHash,
    changedAt = changedAt
)

fun UserPinHistoryDto.toEntity(): UserPinHistory = UserPinHistory(
    id = id ?: 0,
    userId = userId,
    pinHash = pinHash,
    changedAt = changedAt ?: System.currentTimeMillis()
)

fun FeedbackWithUserGroup.toDto(): FeedbackWithUserGroupDto = FeedbackWithUserGroupDto(
    feedbackId = if (feedbackId == 0) null else feedbackId,
    message = message,
    timestamp = timestamp,
    userName = userName,
    groupName = groupName
)

fun FeedbackWithUserGroupDto.toEntity(): FeedbackWithUserGroup = FeedbackWithUserGroup(
    feedbackId = feedbackId ?: 0,
    message = message,
    timestamp = timestamp,
    userName = userName,
    groupName = groupName
)

fun Feedback.toDto(): FeedbackDto = FeedbackDto(
    feedbackId = if (feedbackId == 0) null else feedbackId,
    userId = userId,
    message = message,
    timestamp = timestamp

)

fun FeedbackDto.toEntity(): Feedback = Feedback(
    feedbackId = feedbackId ?: 0,
    userId = userId,
    message = message,
    timestamp = timestamp,

    )




fun UserWithGroup.toDto(): UserWithGroupDto = UserWithGroupDto(
    userId = userId,
    firstName = firstName,
    lastName = lastName,
    emailId = emailId,
    phoneNumber = phoneNumber,
    status = status,
    userCode = userCode,
    isPinSet = isPinSet,
    groupId = groupId,
    groupName = groupName,
    moderator = moderator,
    description = description,
    groupStatus = groupStatus
)

// DTO → Room
fun UserWithGroupDto.toEntity(): UserWithGroup = UserWithGroup(
    userId = userId,
    firstName = firstName,
    lastName = lastName,
    emailId = emailId,
    phoneNumber = phoneNumber,
    status = status,
    userCode = userCode,
    isPinSet = isPinSet,
    groupId = groupId,
    groupName = groupName,
    moderator = moderator,
    description = description,
    groupStatus = groupStatus
)




