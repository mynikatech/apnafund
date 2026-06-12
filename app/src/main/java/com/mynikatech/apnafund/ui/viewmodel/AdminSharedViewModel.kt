package com.mynikatech.apnafund.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AdminSharedViewModel : ViewModel() {

    private val _refreshTabs = MutableLiveData<Unit>()
    val refreshTabs: LiveData<Unit> = _refreshTabs

    fun refreshAdminTabs() {
        _refreshTabs.postValue(Unit)
    }
}