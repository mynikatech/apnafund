package com.mynikatech.apnafund.server.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object UserPasswordHistoryTable : Table(name = "user_password_history") {
    val id           = integer("id").autoIncrement()
    val userId       = integer("userId").references(UsersTable.userId, onDelete = ReferenceOption.CASCADE)
    val passwordHash = text("passwordHash")
    val changedAt    = long("changedAt")  // epoch millis (matches Room Long)

    override val primaryKey = PrimaryKey(id)

    init {
        index(isUnique = false, columns = arrayOf(userId))
        index(isUnique = false, columns = arrayOf(changedAt))
    }
}
