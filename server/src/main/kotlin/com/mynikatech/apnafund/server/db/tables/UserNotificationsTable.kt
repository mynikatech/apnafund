package com.mynikatech.apnafund.server.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object UserNotificationsTable : Table(name = "user_notifications") {
    val userNotificationId = integer("userNotificationId").autoIncrement()
    val notificationType   = varchar("notificationType", length = 64)
        .references(TypeTable.typeCode, onDelete = ReferenceOption.CASCADE) // FK to unique column
    val userId             = integer("userId")
        .references(UsersTable.userId, onDelete = ReferenceOption.CASCADE)
    val message            = text("message").nullable()
    val isExpiredFlag      = bool("isExpiredFlag").default(false)
    val publishedFlag      = bool("publishedFlag").default(false)
    val readFlag           = bool("readFlag").default(false)
    val status             = varchar("status", length = 32).default("ACTIVE")

    override val primaryKey = PrimaryKey(userNotificationId)

    init {
        index(false, userId)
        index(false, readFlag)
        index(false, isExpiredFlag)
    }
}
