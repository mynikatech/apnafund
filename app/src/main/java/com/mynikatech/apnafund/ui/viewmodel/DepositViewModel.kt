package com.mynikatech.apnafund.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.mynikatech.apnafund.ApnaFundApplication
import com.mynikatech.apnafund.data.model.Deposits
import com.mynikatech.apnafund.data.model.DepositsWithMemberNames

class DepositViewModel : ViewModel() {

    private val depositRepository = ApnaFundApplication.depositRepository

    suspend fun getAllDepositsForFundForMonthYear(
        fundId: Int,
        month: Int,
        year: Int
    ): List<DepositsWithMemberNames> {
        return depositRepository.getAllDepositForFundForMonthYear(fundId, month, year)

    }

    suspend fun getDepositsForFundAndMember(fundId: Int, userId: Int): List<Deposits> {
        return depositRepository.getDepositForFundAndMember(fundId, userId)
    }

    suspend fun saveOrUpdateAllAndFetch(
        deposits: List<Deposits>,
        fundId: Int,
        month: String,
        year: String
    ): List<DepositsWithMemberNames> {
        return depositRepository.saveOrUpdateAllAndFetch(deposits, fundId, month, year)
    }


}
