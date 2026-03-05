package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.FundMemberWithName
import com.mynikatech.apnafund.net.dto.FundMemberWithNameDto
import kotlin.String

fun FundMemberWithNameDto.toEntity(): FundMemberWithName = FundMemberWithName(
    fundMemberId = fundMemberId,
    userId = userId,
    fundId = fundId,
    joiningDate = joiningDate,
    firstName = firstName,
    lastName = lastName,
    emailId = emailId

)

fun FundMemberWithName.toDto(): FundMemberWithNameDto = FundMemberWithNameDto(
    fundMemberId = fundMemberId,
    userId = userId,
    fundId = fundId,
    joiningDate = joiningDate,
    firstName = firstName,
    lastName = lastName,
    emailId = emailId

)

fun List<FundMemberWithNameDto>.toEntity(): List<FundMemberWithName> = map { it.toEntity() }
fun List<FundMemberWithName>.toDto(): List<FundMemberWithNameDto> = map { it.toDto() }
