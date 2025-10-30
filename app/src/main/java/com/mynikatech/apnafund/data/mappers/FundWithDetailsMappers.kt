package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.FundWithDetails
import com.mynikatech.apnafund.net.dto.FundWithDetailsDto

fun FundWithDetailsDto.toEntity(): FundWithDetails = FundWithDetails(
    fundName                            = fundName,
    fundStartDate                       = fundStartDate,
    fundMaturityDate                    = fundMaturityDate,
    fundPeriod                          = fundPeriod,
    depositionFrequency                 = depositionFrequency,
    recurringDepositAmount              = recurringDepositAmount,
    fundStatus                          = fundStatus,
    loanInterestRate                    = loanInterestRate,
    lateFeeRate                         = lateFeeRate,
    monthlyDepDateBy                    = monthlyDepDateBy,
    groupId                             = groupId,
    fundId                              = fundId,
    moderator                           = moderator,
    fundCode                            = fundCode,
    moderatorFirstName                  = moderatorFirstName,
    moderatorLastName                   = moderatorLastName,
    fundDetailsId                       = fundDetailsId,
    totalExpectedDeposit                = totalExpectedDeposit,
    totalCurrentDeposit                 = totalCurrentDeposit,
    totalCurrentLateFee                 = totalCurrentLateFee,
    totalCurrentInterestCollected       = totalCurrentInterestCollected,
    totalExpectedMaturityAmount         = totalExpectedMaturityAmount,
    totalCurrAmount                     = totalCurrAmount,
    groupName                           = groupName
)

fun FundWithDetails.toDto(): FundWithDetailsDto = FundWithDetailsDto(
    fundName                            = fundName,
    fundStartDate                       = fundStartDate,
    fundMaturityDate                    = fundMaturityDate,
    fundPeriod                          = fundPeriod,
    depositionFrequency                 = depositionFrequency,
    recurringDepositAmount              = recurringDepositAmount,
    fundStatus                          = fundStatus,
    loanInterestRate                    = loanInterestRate,
    lateFeeRate                         = lateFeeRate,
    monthlyDepDateBy                    = monthlyDepDateBy,
    groupId                             = groupId,
    fundId                              = fundId,
    moderator                           = moderator,
    fundCode                            = fundCode,
    moderatorFirstName                  = moderatorFirstName,
    moderatorLastName                   = moderatorLastName,
    fundDetailsId                       = fundDetailsId,
    totalExpectedDeposit                = totalExpectedDeposit,
    totalCurrentDeposit                 = totalCurrentDeposit,
    totalCurrentLateFee                 = totalCurrentLateFee,
    totalCurrentInterestCollected       = totalCurrentInterestCollected,
    totalExpectedMaturityAmount         = totalExpectedMaturityAmount,
    totalCurrAmount                     = totalCurrAmount,
    groupName                           = groupName
)

fun List<FundWithDetailsDto>.toEntity(): List<FundWithDetails> = map { it.toEntity() }
fun List<FundWithDetails>.toDto(): List<FundWithDetailsDto> = map { it.toDto() }