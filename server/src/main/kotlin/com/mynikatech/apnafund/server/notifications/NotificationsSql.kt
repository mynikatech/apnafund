package com.mynikatech.apnafund.server.notifications

import com.mynikatech.apnafund.net.dto.UserNotificationsDto
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.kotlin.BindKotlin
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlQuery

@RegisterKotlinMapper(UserNotificationsDto::class)
interface NotificationsSql {

    // READ
    @SqlQuery("""SELECT * FROM get_user_notifications(:userId)""")
    fun getUserNotifications(@Bind("userId") userId: Int): List<UserNotificationsDto>

    // CREATE -> returns id
    @SqlQuery("""
        SELECT add_user_notification(
          :notificationType, :userId, :message,
          :isExpiredFlag, :publishedFlag, :readFlag, :status
        )
    """)
    fun addUserNotification(@BindKotlin n: UserNotificationsDto): Int

    // UPDATE -> returns boolean
    @SqlQuery("""
        SELECT update_user_notification(
          :userNotificationId, :notificationType, :userId, :message,
          :isExpiredFlag, :publishedFlag, :readFlag, :status
        )
    """)
    fun updateUserNotification(@BindKotlin n: UserNotificationsDto): Boolean

    // DELETE -> returns boolean
    @SqlQuery("""SELECT delete_user_notification(:id)""")
    fun deleteUserNotification(@Bind("id") userNotificationId: Int): Boolean
}
