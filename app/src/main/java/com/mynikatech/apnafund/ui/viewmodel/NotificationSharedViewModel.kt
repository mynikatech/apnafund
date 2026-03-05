package com.mynikatech.apnafund.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
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

    private val _unreadCount = MutableLiveData<Int>()
    val unreadCount: LiveData<Int> = _unreadCount

    fun loadNotifications(userId: Int) {
        viewModelScope.launch {
            val data = repository.getUserNotifications(userId)
            _notifications.value = data
        }
    }

    fun loadUnreadCount(userId: Int) {
        viewModelScope.launch {
            val count = repository.getUnreadNotificationCount(userId)
            _unreadCount.value = count
        }
    }

    fun markAsRead(notificationId: Int, userId: Int) {
        viewModelScope.launch {
            Log.d("NOTIF_VM", "markAsRead called $notificationId")
            val success = repository.markNotificationRead(notificationId)
            if (success) {
                val current = _unreadCount.value ?: 0
                _unreadCount.value = (current - 1).coerceAtLeast(0)
                loadNotifications(userId)
                loadUnreadCount(userId)
            }
        }
    }
}