package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.FundMembers
import com.mynikatech.apnafund.net.dto.FundMembersDto

// -------- FundMembers --------
fun FundMembersDto.toEntity(): FundMembers = FundMembers(
    fundMemberId = fundMemberId ?: 0,
    userId = userId,
    fundId = fundId,
    joiningDate = joiningDate
)

fun FundMembers.toDto(): FundMembersDto = FundMembersDto(
    fundMemberId = fundMemberId,
    userId = userId,
    fundId = fundId,
    joiningDate = joiningDate
)
fun List<FundMembersDto>.toEntity(): List<FundMembers> = map { it.toEntity() }
fun List<FundMembers>.toDto(): List<FundMembersDto> = map { it.toDto() }