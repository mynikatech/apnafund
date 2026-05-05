package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.UserProfile
import com.mynikatech.apnafund.net.dto.UserProfileDto

fun UserProfile.toDto(): UserProfileDto = UserProfileDto(
    userId = userId,
    userName = userName,
    token = token,
    isLoggedIn = isLoggedIn,
    isPinSet = isPinSet,
    firstName = firstName,
    lastName = lastName,
    emailId = emailId,
    phoneNumber = phoneNumber
).apply {
    roles = this@toDto.roles
    groups = this@toDto.groups
    pendingGroups = this@toDto.pendingGroups
}

fun UserProfileDto.toEntity(): UserProfile = UserProfile(
    userId = userId,
    userName = userName,
    token = token,
    isLoggedIn = isLoggedIn,
    isPinSet = isPinSet,
    firstName = firstName,
    lastName = lastName,
    emailId = emailId,
    phoneNumber = phoneNumber
).apply {
    roles = this@toEntity.roles
    groups = this@toEntity.groups
    pendingGroups = this@toEntity.pendingGroups
}

fun List<UserProfileDto>.toEntity(): List<UserProfile> = map { it.toEntity() }