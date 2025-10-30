package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.UserProfile
import com.mynikatech.apnafund.net.dto.UserProfileDto

fun UserProfile.toDto(): UserProfileDto = UserProfileDto(
    userId = userId,
    userName = userName,
    roleId = roleId,
    roleCode = roleCode,
    groupId = groupId,
    groupName = groupName,
    token = token,
    isLoggedIn = isLoggedIn,
    isPinSet = isPinSet,
    firstName = firstName,
    lastName = lastName,
    emailId = emailId,
    phoneNumber = phoneNumber
)

fun UserProfileDto.toEntity(): UserProfile = UserProfile(
    userId = userId,
    userName = userName,
    roleId = roleId,
    roleCode = roleCode,
    groupId = groupId,
    groupName = groupName,
    token = token,
    isLoggedIn = isLoggedIn,
    isPinSet = isPinSet,
    firstName = firstName,
    lastName = lastName,
    emailId = emailId,
    phoneNumber = phoneNumber
)

fun List<UserProfileDto>.toEntity(): List<UserProfile> = map { it.toEntity() }