package com.mynikatech.apnafund.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mynikatech.apnafund.data.model.FundWithDetails

class FundSharedViewModel : ViewModel() {
    private val _selectedFund = MutableLiveData<FundWithDetails?>()
    val selectedFund: LiveData<FundWithDetails?> get() = _selectedFund

    private val _selectedFundId = MutableLiveData<Int?>()
    val selectedFundId: LiveData<Int?> = _selectedFundId


    // ✅ Add refresh trigger
    private val _refreshTrigger = MutableLiveData<Boolean>()
    val fundDataRefreshTrigger: LiveData<Boolean> get() = _refreshTrigger

    fun selectFund(fund: FundWithDetails) {
        _selectedFund.value = fund
    }

    fun triggerRefresh() {
        _refreshTrigger.value = true
    }

    fun resetRefresh() {
        _refreshTrigger.value = false
    }
    fun clearSelectedFund() {
        _selectedFund.value = null
    }

    fun clearSelectedFundId() {
        _selectedFundId.value = null
    }

    fun setSelectedFundId(fundId: Int) {
        _selectedFundId.value = fundId
    }

}