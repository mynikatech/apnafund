package com.mynikatech.apnafund.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mynikatech.apnafund.ApnaFundApplication
import com.mynikatech.apnafund.data.model.UserNotifications
import kotlinx.coroutines.launch

class NotificationSharedViewModel : ViewModel() {
    private val repository = ApnaFundApplication.userSummaryRepository
    private val _notifications = MutableLiveData<List<UserNotifications>?>()
    val notifications: MutableLiveData<List<UserNotifications>?> get() = _notifications

    fun loadNotifications(userId: Int) {
        viewModelScope.launch {
            val data = repository.getUserNotifications(userId)
            _notifications.value = data
        }
    }

}