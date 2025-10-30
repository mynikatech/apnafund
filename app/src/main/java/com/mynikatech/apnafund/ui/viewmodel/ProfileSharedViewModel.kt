package com.mynikatech.apnafund.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ProfileSharedViewModel : ViewModel() {
    private val _displayName = MutableLiveData<String>()
    val displayName: LiveData<String> = _displayName
    fun publishDisplayName(name: String) { _displayName.value = name }
}