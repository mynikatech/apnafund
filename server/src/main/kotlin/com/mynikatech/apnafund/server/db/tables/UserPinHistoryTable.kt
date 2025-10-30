package com.mynikatech.apnafund.server.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object UserPinHistoryTable : Table("user_pin_history") {
    val id        = integer("id").autoIncrement()
    val userId     = integer("userId").references(UsersTable.userId, onDelete = ReferenceOption.CASCADE)
    val pinHash       = varchar("pinHash", 255)
    val changedAt    = long("changedAt")

    override val primaryKey = PrimaryKey(id)

    init {
        index(isUnique = false, columns = arrayOf(userId))
        index(isUnique = false, columns = arrayOf(changedAt))
    }
}
