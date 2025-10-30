package com.mynikatech.apnafund.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GroupSharedViewModel : ViewModel() {
    private val _selectedGroupId = MutableLiveData<Int>()
    val selectedGroupId: LiveData<Int> get() = _selectedGroupId

    fun setSelectedGroupId(groupId: Int) {
        _selectedGroupId.value = groupId
    }
}