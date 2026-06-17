package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.*
import com.mynikatech.apnafund.net.dto.FundDetailsDto
import com.mynikatech.apnafund.net.dto.FundsDto
import kotlin.Boolean

// -------- Funds --------
fun FundsDto.toEntity(): Funds = Funds(
    fundId = fundId ?: 0,
    fundName = fundName,
    fundStartDate = fundStartDate,
    fundMaturityDate = fundMaturityDate,
    fundPeriod = fundPeriod,
    depositionFrequency = depositionFrequency,
    moderator = moderator,
    recurringDepositAmount = recurringDepositAmount,
    fundStatus = fundStatus,
    loanInterestRate = loanInterestRate,
    hasVariableInterestRate= hasVariableInterestRate,
    revisedLoanInterestRate = revisedLoanInterestRate,
    interestRateRevisionAfterMonths = interestRateRevisionAfterMonths,
    lateFeeRate = lateFeeRate,
    monthlyDepDateBy = monthlyDepDateBy,
    groupId = groupId,
    fundCode = fundCode
)

fun Funds.toDto(): FundsDto = FundsDto(
    fundId = fundId,
    fundName = fundName,
    fundStartDate = fundStartDate,
    fundMaturityDate = fundMaturityDate,
    fundPeriod = fundPeriod,
    depositionFrequency = depositionFrequency,
    moderator = moderator,
    recurringDepositAmount = recurringDepositAmount,
    fundStatus = fundStatus,
    loanInterestRate = loanInterestRate,
    hasVariableInterestRate= hasVariableInterestRate,
    revisedLoanInterestRate = revisedLoanInterestRate,
    interestRateRevisionAfterMonths = interestRateRevisionAfterMonths,
    lateFeeRate = lateFeeRate,
    monthlyDepDateBy = monthlyDepDateBy,
    groupId = groupId,
    fundCode = fundCode
)


fun List<FundsDto>.toEntity(): List<Funds> = map { it.toEntity() }
fun List<Funds>.toDto(): List<FundsDto> = map { it.toDto() }

// -------- FundDetails --------
fun FundDetailsDto.toEntity(): FundDetails = FundDetails(
    fundDetailsId = fundDetailsId ?: 0,
    fundId = fundId,
    totalExpectedDeposit = totalExpectedDeposit,
    totalCurrentDeposit = totalCurrentDeposit,
    totalCurrentLateFee = totalCurrentLateFee,
    totalCurrentInterestCollected = totalCurrentInterestCollected,
    totalExpectedMaturityAmount = totalExpectedMaturityAmount,
    totalCurrAmount = totalCurrAmount
)

fun FundDetails.toDto(): FundDetailsDto = FundDetailsDto(
    fundDetailsId = fundDetailsId,
    fundId = fundId,
    totalExpectedDeposit = totalExpectedDeposit,
    totalCurrentDeposit = totalCurrentDeposit,
    totalCurrentLateFee = totalCurrentLateFee,
    totalCurrentInterestCollected = totalCurrentInterestCollected,
    totalExpectedMaturityAmount = totalExpectedMaturityAmount,
    totalCurrAmount = totalCurrAmount
)






