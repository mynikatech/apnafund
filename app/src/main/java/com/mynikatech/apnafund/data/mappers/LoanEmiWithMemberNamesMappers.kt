package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.LoanEmiWithMemberNames
import com.mynikatech.apnafund.net.dto.LoanEmiWithMemberNamesDto

fun LoanEmiWithMemberNamesDto.toEntity(): LoanEmiWithMemberNames =
    LoanEmiWithMemberNames(
        loanEmiId = loanEmiId,
        loanId = loanId,
        emiMonth = emiMonth,
        emiYear = emiYear,
        emiDepositedDate = emiDepositedDate,
        emiDepositedAmount = emiDepositedAmount,
        prepaymentAmount = prepaymentAmount,
        lateFee = lateFee,
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
        totalInterest = totalInterest,
        currTotalIntPaid = currTotalIntPaid,
        firstName = firstName,
        lastName = lastName,
        userId = userId,
        closedDate = closedDate,
        closureType = closureType,
        closureSource = closureSource
    ).also {
        // UI-only: keep default isEdited=false on the projection object
        it.isEdited = false
    }

fun LoanEmiWithMemberNames.toDto(): LoanEmiWithMemberNamesDto =
    LoanEmiWithMemberNamesDto(
        loanEmiId = loanEmiId,
        loanId = loanId,
        emiMonth = emiMonth,
        emiYear = emiYear,
        emiDepositedDate = emiDepositedDate,
        emiDepositedAmount = emiDepositedAmount,
        prepaymentAmount = prepaymentAmount,
        lateFee = lateFee,
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
        totalInterest = totalInterest,
        currTotalIntPaid = currTotalIntPaid,
        firstName = firstName,
        lastName = lastName,
        userId = userId,
        closedDate = closedDate,
        closureType = closureType,
        closureSource = closureSource
        // isEdited stays client-only
    )

fun List<LoanEmiWithMemberNamesDto>.toEntity(): List<LoanEmiWithMemberNames> = map { it.toEntity() }
fun List<LoanEmiWithMemberNames>.toDto(): List<LoanEmiWithMemberNamesDto> = map { it.toDto() }