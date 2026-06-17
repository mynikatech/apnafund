package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.LoanDetailsWithMemberNames
import com.mynikatech.apnafund.net.dto.LoanDetailsWithMemberNamesDto

fun LoanDetailsWithMemberNamesDto.toEntity(): LoanDetailsWithMemberNames =
    LoanDetailsWithMemberNames(
        loanId = loanId,
        fundId = fundId,
        loanNumber = loanNumber,
        borrowerId = borrowerId,
        issuedDate = issuedDate,
        period = period,
        loanAmount = loanAmount,
        maturityDate = maturityDate,
        rateOfInterest = rateOfInterest,
        hasVariableInterestRate= hasVariableInterestRate,
        revisedLoanInterestRate = revisedLoanInterestRate,
        interestRateRevisionAfterMonths = interestRateRevisionAfterMonths,
        status = status,
        emiInterest = emiInterest,
        currPrincipal = currPrincipal,
        firstName = firstName,
        lastName = lastName,
        userId = userId,
        loanDetailsId = loanDetailsId,
        currTotalIntPaid = currTotalIntPaid,
        totalInterest = totalInterest,
        workflowStatus = workflowStatus,
        closedDate = closedDate,
        closureType = closureType,
        closureSource = closureSource
    )

fun LoanDetailsWithMemberNames.toDto(): LoanDetailsWithMemberNamesDto =
    LoanDetailsWithMemberNamesDto(
        loanId = loanId,
        fundId = fundId,
        loanNumber = loanNumber,
        borrowerId = borrowerId,
        issuedDate = issuedDate,
        period = period,
        loanAmount = loanAmount,
        maturityDate = maturityDate,
        rateOfInterest = rateOfInterest,
        status = status,
        emiInterest = emiInterest,
        currPrincipal = currPrincipal,
        firstName = firstName,
        lastName = lastName,
        userId = userId,
        loanDetailsId = loanDetailsId,
        currTotalIntPaid = currTotalIntPaid,
        totalInterest = totalInterest,
        workflowStatus = workflowStatus,
        closedDate = closedDate,
        closureType = closureType,
        closureSource = closureSource
    )

fun List<LoanDetailsWithMemberNamesDto>.toEntity(): List<LoanDetailsWithMemberNames> = map { it.toEntity() }
fun List<LoanDetailsWithMemberNames>.toDto(): List<LoanDetailsWithMemberNamesDto> =
    map { it.toDto() }