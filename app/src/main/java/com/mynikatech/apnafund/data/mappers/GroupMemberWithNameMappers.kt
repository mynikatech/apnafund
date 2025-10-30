package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.GroupMemberWithName
import com.mynikatech.apnafund.net.dto.GroupMemberWithNameDto

fun GroupMemberWithNameDto.toEntity(): GroupMemberWithName = GroupMemberWithName(
    groupId = groupId,
    groupMemberId = groupMemberId ?: 0,
    userId = userId,
    joiningDate = joiningDate,
    firstName = firstName,
    lastName = lastName

)

fun GroupMemberWithName.toDto(): GroupMemberWithNameDto = GroupMemberWithNameDto(
    groupId = groupId,
    groupMemberId = groupMemberId ?: 0,
    userId = userId,
    joiningDate = joiningDate,
    firstName = firstName,
    lastName = lastName
)

fun List<GroupMemberWithNameDto>.toEntity(): List<GroupMemberWithName> = map { it.toEntity() }
fun List<GroupMemberWithName>.toDto(): List<GroupMemberWithNameDto> = map { it.toDto() }