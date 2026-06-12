package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.PendingModeratorRequest
import com.mynikatech.apnafund.net.dto.PendingModeratorRequestDto

fun PendingModeratorRequestDto.toEntity(): PendingModeratorRequest = PendingModeratorRequest(
    groupId = groupId,
    userId = userId,
    moderatorName = moderatorName,
    groupName = groupName,
    emailId = emailId,
    phoneNumber = phoneNumber,
    groupDescription     = groupDescription

)

fun PendingModeratorRequest.toDto(): PendingModeratorRequestDto = PendingModeratorRequestDto(
    groupId = groupId,
    userId = userId,
    moderatorName = moderatorName,
    groupName = groupName,
    emailId = emailId,
    phoneNumber = phoneNumber,
    groupDescription  = groupDescription
)
fun List<PendingModeratorRequestDto>.toEntity(): List<PendingModeratorRequest> = map { it.toEntity() }
fun List<PendingModeratorRequest>.toDto(): List<PendingModeratorRequestDto> = map { it.toDto() }