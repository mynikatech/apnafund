package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.net.dto.DepositsDto
import com.mynikatech.apnafund.net.dto.DepositsWithMemberNamesDto
import kotlinx.coroutines.flow.Flow

interface DepositsApi {
    suspend fun getByFund(fundId: Int): List<DepositsWithMemberNamesDto>
    suspend fun get(id: Int): DepositsDto?
    fun getAllDeposits(): Flow<List<DepositsDto>>
    suspend fun getAllDepositsforFund(fundId: Int): List<DepositsDto>
    suspend fun getDepositsForFundMonthYear(fundId: Int, month: Int, year: Int): List<DepositsDto>
    suspend fun getDepositsForFundDepositorMonthYear(
        depositorId: Int,
        fundId: Int,
        month: String,
        year: String
    ): DepositsDto?

    suspend fun getAllDepositsForFundForMonthYear(
        fundId: Int,
        month: String,
        year: String
    ): List<DepositsWithMemberNamesDto>

    suspend fun addDeposits(dto: DepositsDto): Int
    suspend fun addAllDeposits(deposits: List<DepositsDto>)
    suspend fun updateDeposits(id: Int, dto: DepositsDto): Boolean
    suspend fun deleteDeposits(id: Int): Boolean
    suspend fun getDepositForMember(
        fundId: Int,
        depositorId: Int,
        month: String,
        year: String
    ): DepositsDto?

    suspend fun getDepositForMemberForFund(fundId: Int, depositorId: Int): List<DepositsDto>
    suspend fun getByFundMonthYear(
        fundId: Int,
        month: String,
        year: String
    ): List<DepositsWithMemberNamesDto>

    suspend fun saveOrUpdateAllAndFetch(
        deposits: List<DepositsDto>,
        fundId: Int,
        month: String,
        year: String
    ): List<DepositsWithMemberNamesDto>
}