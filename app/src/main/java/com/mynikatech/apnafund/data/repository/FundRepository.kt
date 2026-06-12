package com.mynikatech.apnafund.data.repository

import com.mynikatech.apnafund.data.dao.FundMembersDao
import com.mynikatech.apnafund.data.dao.FundsDao
import com.mynikatech.apnafund.data.mappers.toDto
import com.mynikatech.apnafund.data.mappers.toEntity
import com.mynikatech.apnafund.data.model.FundDetails
import com.mynikatech.apnafund.data.model.FundMemberWithName
import com.mynikatech.apnafund.data.model.FundMembers
import com.mynikatech.apnafund.data.model.FundWithDetails
import com.mynikatech.apnafund.data.model.Funds
import com.mynikatech.apnafund.net.FundsApi
import com.mynikatech.apnafund.net.dto.AvailableFundMemberDto
import com.mynikatech.apnafund.net.dto.CloseFundRequest
import com.mynikatech.apnafund.net.dto.FundAvailabilityDto
import com.mynikatech.apnafund.net.dto.FundMemberFinancialSummaryDto
import com.mynikatech.apnafund.net.dto.FundMembersDto
import com.mynikatech.apnafund.net.dto.FundUpdateRequestDto
import com.mynikatech.apnafund.net.dto.MonthlyFinancialSummaryResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class FundRepository(
    private val fundDao: FundsDao, private val fundMembersDao: FundMembersDao,
    private val api: FundsApi
) {

    fun observeFunds(): Flow<List<Funds>> =
        flow {
            val dtos = api.getAllFunds()
            emit(dtos.map { it.toEntity() })
        }
            .catch { emit(emptyList()) }
            .flowOn(Dispatchers.IO)

    suspend fun updateFund(fund: Funds) {
        api.updateFund(fund.fundId, fund.toDto())
    }

    suspend fun fetchAllActiveFunds(): List<Funds> {
        return api.getAllActiveFunds().toEntity()

    }

    suspend fun fetchAllActiveFundsforGroup(groupId: Int): List<Funds> {
        return api.getAllActiveFundsForGroup(groupId).toEntity()

    }

    suspend fun fetchFundsByGroupAndModerator(groupId: Int, moderatorId: Int): List<Funds> {
        return api.getAllActiveFundsForGroupAndModerator(groupId, moderatorId).toEntity()

    }

    suspend fun fetchFund(fundId: Int): Funds? {
        return api.getFund(fundId)?.toEntity()
    }

    suspend fun saveFundAndDetails(
        fund: Funds,
        fundDetails: FundDetails,
        requestorId: Int,
        excludeCreator: Boolean
    ): Int {
        return api.addFundWithDetails(
            fund.toDto(),
            fundDetails.toDto(),
            requestorId,
            excludeCreator
        )
    }

    suspend fun updateFundDetails(details: FundDetails) {
        api.updateFundDetails(details.toDto())
    }

    suspend fun updateFundAndDetails(fund: Funds, fundDetails: FundDetails) {
        val fundUpdateReq = FundUpdateRequestDto(
            fund = fund.toDto(),
            fundDetails = fundDetails.toDto()
        )
        api.updateFundWithDetails(fundUpdateReq)
    }

    suspend fun getAllFundWithDetails(): List<FundWithDetails> {
        return api.getAllFundsWithDetails().toEntity()
    }

    suspend fun getAllFundWithDetailsForGroup(groupId: Int): List<FundWithDetails> {
        return api.getAllFundsWithDetailsForGroup(groupId).toEntity()
    }

    suspend fun getFundWithDetails(fundId: Int): FundWithDetails {
        return api.getFundWithDetails(fundId).toEntity()
    }

    suspend fun getFundDetails(fundId: Int): FundDetails? {
        return api.getFundDetails(fundId)?.toEntity()
    }

    suspend fun getFund(fundId: Int): Funds? {
        return api.getFund(fundId)?.toEntity()
    }

    suspend fun getAvailableFundAmount(fundId: Int): Double? {
        return api.getAvailableFundAmount(fundId)
    }

    suspend fun getAvailableFundAmounts(fundId: Int): FundAvailabilityDto? {
        return api.getTotalAvailableFundAmount(fundId)
    }

    suspend fun fetchAllMembersforFund(fundId: Int): List<FundMembers> {
        return api.getAllFundMembers(fundId).toEntity()
    }

    suspend fun getAvailableFundMembers(groupId: Int, fundId: Int): List<AvailableFundMemberDto> {
        val memberList: List<AvailableFundMemberDto> = api.getAvailableFundMembers(groupId, fundId)
        return memberList
    }

    suspend fun addFundMembers(fundMembers: List<FundMembers>) {
        api.addFundMembers(fundMembers.toDto())
    }

    suspend fun getFundMembersWithNamesForFund(
        fundId: Int,
        status: String
    ): List<FundMemberWithName> {
        return api.getFundMembersWithNamesForFund(fundId, status).toEntity()
    }

    suspend fun getFundMembersFinancialSummForFund(fundId: Int): List<FundMemberFinancialSummaryDto> {
        return api.getFundMembersFinancialSummForFund(fundId)
    }

    suspend fun getFundMembersMonthlyFinancialSummForFund(fundId: Int, month: Int, year: Int): MonthlyFinancialSummaryResponseDto {
        return api.getFundMembersMonthlyFinancialSummForFund(fundId, month, year)
    }

    suspend fun insertFundDetails(details: FundDetails) {
        api.insertFundDetails(details.toDto())
    }

    suspend fun closeFund(
        fundId: Int,
        userId: Int,
        reason: String
    ) {

        api.closeFund(
            fundId,
            CloseFundRequest(
                closedBy = userId,
                reason = reason
            )
        )
    }

    suspend fun updateFundMember(fundMember: FundMembersDto) {
        api.updateFundMember(fundMember)
    }


}