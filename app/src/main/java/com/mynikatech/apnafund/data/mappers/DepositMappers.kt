package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.DepositRowState
import com.mynikatech.apnafund.data.model.Deposits
import com.mynikatech.apnafund.net.dto.DepositRowStateDto
import com.mynikatech.apnafund.net.dto.DepositsDto

// Room → DTO
fun Deposits.toDto(): DepositsDto = DepositsDto(
    depositId     = if (depositId == 0) null else depositId,
    depositorId   = depositorId,
    depositedDate = depositedDate,
    depositAmount = depositAmount,
    depositMonth  = depositMonth,
    depositYear   = depositYear,
    fundId        = fundId,
    lateFee       = lateFee
)

fun DepositsDto.toEntity(): Deposits = Deposits(
    depositId     = depositId ?:0,
    depositorId   = depositorId,
    depositedDate = depositedDate,
    depositAmount = depositAmount,
    depositMonth  = depositMonth,
    depositYear   = depositYear,
    fundId        = fundId,
    lateFee       = lateFee
)

fun List<DepositsDto>.toEntity(): List<Deposits> = map { it.toEntity() }
fun List<Deposits>.toDto(): List<DepositsDto> = map { it.toDto() }



fun DepositRowStateDto.toEntity(): DepositRowState = DepositRowState(
    depositorId   = depositorId,
    memberName    = memberName,
    depositAmount = depositAmount,
    depositDate   = depositDate,
    lateFee       = lateFee,
    isEdited      = isEdited

)

fun DepositRowState.toDto(): DepositRowStateDto = DepositRowStateDto(
    depositorId   = depositorId,
    memberName    = memberName,
    depositAmount = depositAmount,
    depositDate   = depositDate,
    lateFee       = lateFee,
    isEdited      = isEdited
)
