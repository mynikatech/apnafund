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
import com.mynikatech.apnafund.data.model.Users
import com.mynikatech.apnafund.net.FundsApi
import com.mynikatech.apnafund.net.dto.CloseFundRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
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

    suspend fun createFundRemoteAndCache(f: Funds): Int = withContext(Dispatchers.IO) {
        val id = api.addFund(f.toDto())
        //fundDao.addFund(f.copy(fundId = id))
        id
    }

    suspend fun updateFundRemoteAndCache(f: Funds): Boolean = withContext(Dispatchers.IO) {
        val ok = api.updateFund(f.fundId, f.toDto())
        //if (ok) fundDao.updateFund(f)
        ok
    }

    suspend fun deleteFundRemoteAndCache(id: Int): Boolean = withContext(Dispatchers.IO) {
        val ok = api.deleteFund(id)
       // if (ok) fundDao.getFundOrNull(id)?.let { fundDao.deleteFund(it) }
        ok
    }

    // Members
    suspend fun refreshMembers(fundId: Int) = withContext(Dispatchers.IO) {
        runCatching { api.getAllFundMembers(fundId) }.getOrNull()
    }

    /** Network-only: fetch details (no local writes). */
    suspend fun refreshDetails(fundId: Int) = withContext(Dispatchers.IO) {
        runCatching { api.getFundDetails(fundId) }.getOrNull()
    }

    suspend fun createFund(fund: Funds) {
        api.addFund(fund.toDto())
    }

    suspend fun deleteFund(fund: Funds) {
        api.deleteFund(fund.fundId)
    }

    suspend fun updateFund(fund: Funds) {
        api.updateFund(fund.fundId, fund.toDto())
    }

    suspend fun fetchAllFunds(): List<Funds> {
        return api.getAllFunds().toEntity()

    }

    suspend fun fetchAllActiveFunds(): List<Funds> {
        return api.getAllActiveFunds().toEntity()

    }

    suspend fun fetchAllActiveFundsforGroup(groupId: Int): List<Funds> {
        return api.getAllActiveFundsForGroup(groupId).toEntity()

    }

    suspend fun fetchFund(fundId: Int): Funds? {
        return api.getFund(fundId)?.toEntity()
    }

    suspend fun saveFundAndDetails(fund: Funds, fundDetails: FundDetails): Int {
        return api.addFundWithDetails(fund.toDto(), fundDetails.toDto())
    }

    suspend fun updateFundDetails(details: FundDetails) {
        api.updateFundDetails(details.toDto())
    }

    suspend fun updateFundAndDetails(fund: Funds, fundDetails: FundDetails) {
        api.updateFundWithDetails(fund.toDto(), fundDetails.toDto())
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

    suspend fun fetchAllMembersforFund(fundId: Int): List<FundMembers> {
        return api.getAllFundMembers(fundId).toEntity()
    }

    suspend fun getAvailableFundMembers(groupId: Int, fundId: Int): List<Users> {
        val memberList: List<Users> = api.getAvailableFundMembers(groupId, fundId).toEntity()
        return memberList
    }

    suspend fun addFundMembers(fundMembers: List<FundMembers>) {
        api.addFundMembers(fundMembers.toDto())
    }

    suspend fun getFundMembersWithNamesForFund(fundId: Int): List<FundMemberWithName> {
        return api.getFundMembersWithNamesForFund(fundId).toEntity()
    }

    suspend fun checkIfFundMemberAlreadyAdded(userId: Int, fundId: Int): Boolean {
        return api.checkIfFundMemberAlreadyAdded(userId, fundId)
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


}