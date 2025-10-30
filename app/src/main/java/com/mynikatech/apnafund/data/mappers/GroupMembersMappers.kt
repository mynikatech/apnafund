package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.GroupMembers
import com.mynikatech.apnafund.net.dto.GroupMembersDto

fun GroupMembersDto.toEntity(): GroupMembers = GroupMembers(
    groupId = groupId,
    groupMemberId = groupMemberId ?: 0,
    userId = userId,
    joiningDate = joiningDate,

    )

fun GroupMembers.toDto(): GroupMembersDto = GroupMembersDto(
    groupId = groupId,
    groupMemberId = groupMemberId ?: 0,
    userId = userId,
    joiningDate = joiningDate,
)

fun List<GroupMembersDto>.toEntity(): List<GroupMembers> = map { it.toEntity() }
fun List<GroupMembers>.toDto(): List<GroupMembersDto> = map { it.toDto() }