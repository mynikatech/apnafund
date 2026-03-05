package com.mynikatech.apnafund.data.mappers

import com.mynikatech.apnafund.data.model.UserNotifications
import com.mynikatech.apnafund.net.dto.UserNotificationsDto

fun UserNotifications.toDto(): UserNotificationsDto = UserNotificationsDto(

    userNotificationId = userNotificationId,
    notificationType = notificationType,
    userId = userId,
    message = message,
    isExpiredFlag = isExpiredFlag,
    publishedFlag = publishedFlag,
    readFlag = readFlag,
    status = status,
    createdAt = createdAt,
    readAt = readAt,
    expiredAt = expiredAt

)

// DTO → Room
fun UserNotificationsDto.toEntity(): UserNotifications = UserNotifications(

    userNotificationId = userNotificationId ?: 0,
    notificationType = notificationType,
    userId = userId,
    message = message,
    isExpiredFlag = isExpiredFlag,
    publishedFlag = publishedFlag,
    readFlag = readFlag,
    status = status,
    createdAt = createdAt,
    readAt = readAt,
    expiredAt = expiredAt
)

@JvmName("toUserNotificationsEntity")
fun List<UserNotificationsDto>.toEntity(): List<UserNotifications> =
    map { d ->
        UserNotifications(
            userNotificationId = d.userNotificationId ?: 0,
            notificationType = d.notificationType,
            userId = d.userId,
            message = d.message,
            isExpiredFlag = d.isExpiredFlag,
            publishedFlag = d.publishedFlag,
            readFlag = d.readFlag,
            status = d.status,
            createdAt = d.createdAt,
            readAt = d.readAt,
            expiredAt = d.expiredAt
        )
    }

fun List<UserNotifications>.toDto(): List<UserNotificationsDto> = map { d ->
    UserNotificationsDto(
        userNotificationId = d.userNotificationId,
        notificationType = d.notificationType,
        userId = d.userId,
        message = d.message,
        isExpiredFlag = d.isExpiredFlag,
        publishedFlag = d.publishedFlag,
        readFlag = d.readFlag,
        status = d.status,
        createdAt = d.createdAt,
        readAt = d.readAt,
        expiredAt = d.expiredAt
    )
}