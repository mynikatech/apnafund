package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.net.dto.UserNotificationsDto

interface NotificationsApi {

    suspend fun addNotifications(userNotifications: UserNotificationsDto)
    suspend fun updateNotifications(userNotifications: UserNotificationsDto)
    suspend fun deleteNotifications(userNotifications: UserNotificationsDto)
    suspend fun getUserNotifications(userId: Int): List<UserNotificationsDto>?
}