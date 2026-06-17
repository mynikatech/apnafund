package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.Loans
import com.mynikatech.apnafund.net.dto.*
import kotlin.String

fun LoansDto.toEntity(): Loans = Loans(
    loanId         = loanId ?: 0,
    loanNumber     = loanNumber,
    borrowerId     = borrowerId,
    issuedDate     = issuedDate,
    period         = period,
    loanAmount     = loanAmount,
    maturityDate   = maturityDate,
    rateOfInterest = rateOfInterest,
    hasVariableInterestRate= hasVariableInterestRate,
    revisedLoanInterestRate = revisedLoanInterestRate,
    interestRateRevisionAfterMonths = interestRateRevisionAfterMonths,
    status         = status,
    fundId         = fundId,
    workflowStatus = workflowStatus,
    closedDate     = closedDate,
    closureType    = closureType,
    closureSource  = closureSource

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
    hasVariableInterestRate= hasVariableInterestRate,
    revisedLoanInterestRate = revisedLoanInterestRate,
    interestRateRevisionAfterMonths = interestRateRevisionAfterMonths,
    status         = status,
    workflowStatus = workflowStatus,
    closedDate     = closedDate,
    closureType    = closureType,
    closureSource  = closureSource
)

fun List<LoansDto>.toEntity(): List<Loans> = map { it.toEntity() }
fun List<Loans>.toDto(): List<LoansDto> = map { it.toDto() }







