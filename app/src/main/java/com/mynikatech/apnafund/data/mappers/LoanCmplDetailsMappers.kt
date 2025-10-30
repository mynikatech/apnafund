package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.LoanCmplDetails
import com.mynikatech.apnafund.net.dto.LoanCmplDetailsDto

fun LoanCmplDetailsDto.toProjection(): LoanCmplDetails = LoanCmplDetails(
    loanId          = loanId,
    fundId          = fundId,
    loanDetailsId   = loanDetailsId,
    loanNumber      = loanNumber,
    borrowerId      = borrowerId,
    issuedDate      = issuedDate,
    period          = period,
    loanAmount      = loanAmount,
    maturityDate    = maturityDate,
    rateOfInterest  = rateOfInterest,
    status          = status,
    emiInterest     = emiInterest,
    totalAmount     = totalAmount,
    currPrincipal   = currPrincipal,
    origPrincipal   = origPrincipal,
    totalInterest   = totalInterest,
    currTotalIntPaid= currTotalIntPaid
)

fun LoanCmplDetails.toDto(): LoanCmplDetailsDto = LoanCmplDetailsDto(
    loanId          = loanId,
    fundId          = fundId,
    loanDetailsId   = loanDetailsId,
    loanNumber      = loanNumber,
    borrowerId      = borrowerId,
    issuedDate      = issuedDate,
    period          = period,
    loanAmount      = loanAmount,
    maturityDate    = maturityDate,
    rateOfInterest  = rateOfInterest,
    status          = status,
    emiInterest     = emiInterest,
    totalAmount     = totalAmount,
    currPrincipal   = currPrincipal,
    origPrincipal   = origPrincipal,
    totalInterest   = totalInterest,
    currTotalIntPaid= currTotalIntPaid
)