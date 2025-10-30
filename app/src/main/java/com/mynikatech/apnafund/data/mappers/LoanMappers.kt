package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.Loans
import com.mynikatech.apnafund.net.dto.*

fun LoansDto.toEntity(): Loans = Loans(
    loanId         = loanId ?: 0,
    loanNumber     = loanNumber,
    borrowerId     = borrowerId,
    issuedDate     = issuedDate,
    period         = period,
    loanAmount     = loanAmount,
    maturityDate   = maturityDate,
    rateOfInterest = rateOfInterest,
    status         = status,
    fundId         = fundId
)

fun Loans.toDto(): LoansDto = LoansDto(
    loanId         = loanId,
    fundId         = fundId,
    loanNumber     = loanNumber,
    borrowerId     = borrowerId,
    issuedDate     = issuedDate,
    period         = period,
    loanAmount     = loanAmount,
    maturityDate   = maturityDate,
    rateOfInterest = rateOfInterest,
    status         = status
)

fun List<LoansDto>.toEntity(): List<Loans> = map { it.toEntity() }
fun List<Loans>.toDto(): List<LoansDto> = map { it.toDto() }







