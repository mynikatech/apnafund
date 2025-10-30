package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.LoanDetails
import com.mynikatech.apnafund.net.dto.LoanDetailsDto

fun LoanDetailsDto.toEntity(): LoanDetails = LoanDetails(
    loanDetailsId   = loanDetailsId ?: 0,
    loanId          = loanId,
    origPrincipal   = origPrincipal,
    totalInterest   = totalInterest,
    totalAmount     = totalAmount,
    emiInterest     = emiInterest,
    currTotalIntPaid= currTotalIntPaid,
    currPrincipal   = currPrincipal
)

fun LoanDetails.toDto(): LoanDetailsDto = LoanDetailsDto(
    loanDetailsId   = loanDetailsId,
    loanId          = loanId,
    origPrincipal   = origPrincipal,
    totalInterest   = totalInterest,
    totalAmount     = totalAmount,
    emiInterest     = emiInterest,
    currTotalIntPaid= currTotalIntPaid,
    currPrincipal   = currPrincipal
)

fun List<LoanDetailsDto>.toEntity(): List<LoanDetails> = map { it.toEntity() }
fun List<LoanDetails>.toDto(): List<LoanDetailsDto> = map { it.toDto() }