package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class FundsDto(
    val fundId: Int? = null,
    val fundName: String,
    val fundStartDate: String,
    val fundMaturityDate: String,
    val fundPeriod: Double,
    val depositionFrequency: String,
    val moderator: Int,
    val recurringDepositAmount: Double,
    val fundStatus: String = "ACTIVE",
    val loanInterestRate: Double,
    val lateFeeRate: Double,
    val monthlyDepDateBy: Int,
    val groupId: Int,
    val fundCode: String
)