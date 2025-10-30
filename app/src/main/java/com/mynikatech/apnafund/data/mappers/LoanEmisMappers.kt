package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.LoanEmis
import com.mynikatech.apnafund.net.dto.LoanEmisDto

fun LoanEmisDto.toEntity(): LoanEmis = LoanEmis(
    loanEmiId          = loanEmiId ?: 0,
    loanId             = loanId,
    emiMonth           = emiMonth,
    emiYear            = emiYear,
    emiDepositedDate   = emiDepositedDate,
    emiDepositedAmount = emiDepositedAmount,
    prepaymentAmount   = prepaymentAmount,
    lateFee            = lateFee
)

fun LoanEmis.toDto(): LoanEmisDto = LoanEmisDto(
    loanEmiId          = loanEmiId,
    loanId             = loanId,
    emiMonth           = emiMonth,
    emiYear            = emiYear,
    emiDepositedDate   = emiDepositedDate,
    emiDepositedAmount = emiDepositedAmount,
    prepaymentAmount   = prepaymentAmount,
    lateFee            = lateFee
)

fun List<LoanEmisDto>.toEntity(): List<LoanEmis> = map { it.toEntity() }
fun List<LoanEmis>.toDto(): List<LoanEmisDto> = map { it.toDto() }
