package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.DepositsWithMemberNames
import com.mynikatech.apnafund.net.dto.DepositsWithMemberNamesDto

// “with names” DTO → a safe Room entity (fallback for caching)
fun DepositsWithMemberNamesDto.toEntity(): DepositsWithMemberNames = DepositsWithMemberNames(
    depositId = depositId ?: 0,
    depositorId = userId,
    depositedDate = depositedDate,
    depositAmount = depositAmount,
    depositMonth = depositMonth,
    depositYear = depositYear,
    fundId = fundId ?: 0,
    lateFee = lateFee,
    firstName = firstName,
    lastName = lastName,
    userId = userId
)

fun DepositsWithMemberNames.toDto(): DepositsWithMemberNamesDto = DepositsWithMemberNamesDto(
    depositId = depositId ?: 0,
    depositorId = userId,
    depositedDate = depositedDate ?: "",
    depositAmount = depositAmount ?: 0.0,
    depositMonth = depositMonth ?: "",
    depositYear = depositYear ?: "",
    fundId = fundId ?: 0,
    lateFee = lateFee,
    firstName = firstName,
    lastName = lastName,
    userId = userId
)

fun List<DepositsWithMemberNamesDto>.toEntity(): List<DepositsWithMemberNames> =
    map { it.toEntity() }

fun List<DepositsWithMemberNames>.toDto(): List<DepositsWithMemberNamesDto> = map { it.toDto() }