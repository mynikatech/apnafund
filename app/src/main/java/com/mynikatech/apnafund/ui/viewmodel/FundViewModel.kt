package com.mynikatech.apnafund.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.mynikatech.apnafund.ApnaFundApplication
import com.mynikatech.apnafund.data.model.FundDetails
import com.mynikatech.apnafund.data.model.FundMemberWithName
import com.mynikatech.apnafund.data.model.FundMembers
import com.mynikatech.apnafund.data.model.FundWithDetails
import com.mynikatech.apnafund.data.model.Funds
import com.mynikatech.apnafund.data.model.Users
import com.mynikatech.apnafund.util.ApnaBankDate


class FundViewModel : ViewModel() {

    private val fundRepository = ApnaFundApplication.fundRepository

    suspend fun fetchAllActiveFunds(isAdmin: Boolean, moderatorGroupId: Int): List<Funds> {
        return if (isAdmin)
            fundRepository.fetchAllActiveFunds()
        else
            fundRepository.fetchAllActiveFundsforGroup(moderatorGroupId)
    }

    suspend fun fetchAllFundsWithDetails(
        isAdmin: Boolean,
        moderatorGroupId: Int
    ): List<FundWithDetails> {
        return if (isAdmin) {
            fundRepository.getAllFundWithDetails()
        } else {
            fundRepository.getAllFundWithDetailsForGroup(moderatorGroupId)
        }
    }

    suspend fun fetchFund(fundId: Int): Funds? {
        val fund = fundRepository.fetchFund(fundId)
        return fund
    }

    suspend fun saveOrUpdateFund(
        fundToSave: Funds,
        fundDetailstoSave: FundDetails,
        groupId: Int,
        recalculateFinance: Boolean = false
    ): Int {
        // For now only allow Fund Name and Group to Edit post a first deposit is completed.
        // Later will work on allowing to update the other fields like period, deposit amount, start date etc
        var newFundId = fundToSave.fundId
        var totNoOfMembers = 0
        var updatedTotalExpectedDeposit = 0.0

        if (fundToSave.fundId > 0 && !recalculateFinance) {
            fundRepository.updateFund(fundToSave)
        } else if (fundToSave.fundId > 0) {
            // Get total fund members
            totNoOfMembers = fundRepository.fetchAllMembersforFund(fundToSave.fundId).size
            updatedTotalExpectedDeposit =
                fundToSave.recurringDepositAmount * totNoOfMembers * fundToSave.fundPeriod
            val updatedFundDetails = fundDetailstoSave.copy(
                totalExpectedDeposit = updatedTotalExpectedDeposit,
                totalExpectedMaturityAmount = updatedTotalExpectedDeposit
            )
            fundRepository.updateFundAndDetails(fundToSave, updatedFundDetails)
        } else {
            newFundId = fundRepository.saveFundAndDetails(fundToSave, fundDetailstoSave)
        }
        return newFundId
    }

    suspend fun fetchFundMembersforFund(fundId: Int): List<FundMembers> {
        val fundMembers = fundRepository.fetchAllMembersforFund(fundId)
        return fundMembers
    }

    suspend fun getFundMembersWithNamesForFund(fundId: Int): List<FundMemberWithName> {
        return fundRepository.getFundMembersWithNamesForFund(fundId)
    }

    suspend fun getAvailableFundMembers(groupId: Int, fundId: Int): List<Users> {
        val memberList: List<Users> = fundRepository.getAvailableFundMembers(groupId, fundId)
        return memberList
    }

    suspend fun addFundMembers(fundId: Int, selectedUserIds: List<Int>) {
        // get existing number of fund members
        if(selectedUserIds.isNotEmpty()) {
            val totExitingFundMembers = fundRepository.fetchAllMembersforFund(fundId).size
            val existingFundDetails = fundRepository.getFundDetails(fundId)
            val existingFund = fundRepository.getFund(fundId)!!
            val totUpdatedFundMembers = totExitingFundMembers + selectedUserIds.size
            val listOfNewFundMembers = ArrayList<FundMembers>()
            selectedUserIds.forEach {
                val fundMember = FundMembers(
                    userId = it,
                    fundId = fundId,
                    joiningDate = ApnaBankDate.getCurrentDate()
                )
                listOfNewFundMembers.add(fundMember)
            }
            fundRepository.addFundMembers(listOfNewFundMembers)
            val updatedTotalExpectedDeposit =
                existingFund.recurringDepositAmount * totUpdatedFundMembers * existingFund.fundPeriod
            if (null != existingFundDetails) {
                val updatedFundDetails = existingFundDetails.copy(
                    totalExpectedDeposit = updatedTotalExpectedDeposit,
                    totalExpectedMaturityAmount = updatedTotalExpectedDeposit
                )
                fundRepository.updateFundDetails(updatedFundDetails)
            } else {
                val newFundDetails = FundDetails(
                    fundId = fundId,
                    totalExpectedDeposit = updatedTotalExpectedDeposit,
                    totalCurrentDeposit = 0.0,
                    totalCurrentLateFee = 0.0,
                    totalCurrentInterestCollected = 0.0,
                    totalExpectedMaturityAmount = updatedTotalExpectedDeposit, // defaulted as no loans interest or fee, this will change
                    totalCurrAmount = 0.0

                )
                fundRepository.insertFundDetails(newFundDetails)
            }
        }
    }

    suspend fun getFundDetails(fundId: Int): FundWithDetails {
        return fundRepository.getFundWithDetails(fundId)
    }

    suspend fun getMemberByNameForFund(name: String, fundId: Int): FundMemberWithName? {
        val members: List<FundMemberWithName> =
            fundRepository.getFundMembersWithNamesForFund(fundId)
        return members.firstOrNull {
            "${it.firstName} ${it.lastName}".trim()
                .equals(name.trim(), ignoreCase = true)
        }
    }

}