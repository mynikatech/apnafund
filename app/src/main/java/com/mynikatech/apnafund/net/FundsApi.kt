package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.data.model.FundMembers
import com.mynikatech.apnafund.net.dto.AvailableFundMemberDto
import com.mynikatech.apnafund.net.dto.CloseFundRequest
import com.mynikatech.apnafund.net.dto.FundAvailabilityDto
import com.mynikatech.apnafund.net.dto.FundDetailsDto
import com.mynikatech.apnafund.net.dto.FundMemberWithNameDto
import com.mynikatech.apnafund.net.dto.FundsDto
import com.mynikatech.apnafund.net.dto.FundMembersDto
import com.mynikatech.apnafund.net.dto.FundUpdateRequestDto
import com.mynikatech.apnafund.net.dto.FundWithDetailsDto
import com.mynikatech.apnafund.net.dto.UsersDto
import kotlinx.coroutines.flow.Flow

interface FundsApi {
    suspend fun getAllFunds(): List<FundsDto>
    suspend fun getAllActiveFunds(): List<FundsDto>
    suspend fun getFundbyCode(fundCode: String): Int
    suspend fun getAllActiveFundsForGroup(groupId: Int): List<FundsDto>
    suspend fun getAllActiveFundsForGroupAndModerator(groupId: Int, userId: Int): List<FundsDto>
    suspend fun getAllFundsWithDetails(): List<FundWithDetailsDto>
    suspend fun getAllFundsWithDetailsForGroup(groupId: Int): List<FundWithDetailsDto>
    suspend fun getFundWithDetails(fundId: Int): FundWithDetailsDto
    suspend fun getFund(id: Int): FundsDto?
    suspend fun getFundDetails(fundId: Int): FundDetailsDto?
    fun getAllFundsforGroup(groupId: Int): Flow<List<FundsDto>>
    suspend fun addFund(dto: FundsDto): Int
    suspend fun updateFund(id: Int, dto: FundsDto)
    suspend fun deleteFund(id: Int): Boolean
    suspend fun getRateOfInterestforFund(fundId: Int): Double
    suspend fun addFundWithDetails(dto: FundsDto, details: FundDetailsDto, requestorId: Int, excludeCreator: Boolean): Int

    // Members
    suspend fun getAllFundMembers(fundId: Int): List<FundMembersDto>
    suspend fun getFundMembersWithNamesForFund(fundId: Int): List<FundMemberWithNameDto>
    suspend fun addFundMember(dto: FundMembersDto): Int
    suspend fun addFundMembers(fundMembers: List<FundMembersDto>): List<Int>
    suspend fun removeFundMember(fundMemberId: Int): Boolean
    suspend fun checkIfFundMemberAlreadyAdded(userId: Int, fundId: Int): Boolean
    suspend fun updateFundMember(fundMember: FundMembersDto)

    // Details
    suspend fun insertFundDetails(details: FundDetailsDto)
    suspend fun updateFundDetails(details: FundDetailsDto)
    suspend fun upsertDetails(dto: FundDetailsDto): Int
    suspend fun getAvailableFundAmount(fundId: Int): Double?
    suspend fun getTotalAvailableFundAmount(fundId: Int): FundAvailabilityDto?
    suspend fun getAvailableFundMembers(groupId: Int, fundId: Int): List<AvailableFundMemberDto>
    suspend fun updateFundWithDetails(fundUpdatereq: FundUpdateRequestDto)
    suspend fun closeFund(fundId: Int, request: CloseFundRequest )
}
