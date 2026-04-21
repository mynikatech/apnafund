package com.mynikatech.apnafund.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mynikatech.apnafund.ApnaFundApplication
import com.mynikatech.apnafund.data.model.FundWithDetails
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FundSharedViewModel : ViewModel() {
    private val fundRepository = ApnaFundApplication.fundRepository
    private val _selectedFund = MutableLiveData<FundWithDetails?>()
    val selectedFund: LiveData<FundWithDetails?> get() = _selectedFund

    private val _selectedFundId = MutableLiveData<Int?>()
    val selectedFundId: LiveData<Int?> = _selectedFundId

    private val _funds = MutableStateFlow<List<FundWithDetails>>(emptyList())
    val funds = _funds.asStateFlow()
    private var isLoaded = false
    private var lastIsAdmin: Boolean? = null
    private var lastGroupId: Int? = null


    // Add refresh trigger
    private val _refreshTrigger = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val refreshTrigger = _refreshTrigger.asSharedFlow()

    fun selectFund(fund: FundWithDetails) {
        _selectedFund.value = fund
    }

    suspend fun triggerRefresh() {
        _refreshTrigger.emit(Unit)
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

    fun loadFunds(isAdmin: Boolean, groupId: Int) {
        if (isLoaded && lastIsAdmin == isAdmin && lastGroupId == groupId) return

        viewModelScope.launch {
            lastIsAdmin = isAdmin
            lastGroupId = groupId

            _funds.value = fetchFunds(isAdmin, groupId)
            isLoaded = true
        }
    }

    fun refreshFunds() {
        viewModelScope.launch {
            val isAdmin = lastIsAdmin ?: return@launch
            val groupId = lastGroupId ?: return@launch

            _funds.value = fetchFunds(isAdmin, groupId)
            isLoaded = true
        }
    }

    private suspend fun fetchFunds(isAdmin: Boolean, groupId: Int): List<FundWithDetails> {
        return if (isAdmin) {
            fundRepository.getAllFundWithDetails()
        } else {
            fundRepository.getAllFundWithDetailsForGroup(groupId)
        }
    }

    fun clearCache() {
        isLoaded = false
        lastIsAdmin = null
        lastGroupId = null
        _funds.value = emptyList()
    }

}