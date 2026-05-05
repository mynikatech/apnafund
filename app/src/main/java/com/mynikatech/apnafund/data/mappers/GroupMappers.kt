package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.Groups
import com.mynikatech.apnafund.net.dto.GroupsDto
import com.mynikatech.apnafund.net.dto.GroupsWithModeratorDto

fun GroupsDto.toEntity(): Groups = Groups(
    groupId = this.groupId ?: 0, // Room will auto-gen if needed
    groupName = this.groupName,
    moderator = this.moderator,
    createdDate = this.createdDate,
    description = this.description,
    groupCode = this.groupCode,
    status = this.status
)

fun Groups.toDto(): GroupsDto = GroupsDto(
    groupId = this.groupId,
    groupName = this.groupName,
    moderator = this.moderator,
    createdDate = this.createdDate,
    description = this.description,
    groupCode = this.groupCode,
    status = this.status
)

fun List<GroupsDto>.toEntity(): List<Groups> = map { it.toEntity() }
fun List<Groups>.toDto(): List<GroupsDto> = map { it.toDto() }


fun GroupsWithModeratorDto.toEntity(): Groups {
    return Groups(
        groupId = this.groupId,
        groupName = this.groupName,
        moderator = this.moderator,
        createdDate = this.createdDate,
        description = this.description,
        groupCode = this.groupCode,
        status = this.status
    )
}

fun List<GroupsWithModeratorDto>.toGroupsEntityFromDto(): List<Groups> = map { it.toEntity() }


