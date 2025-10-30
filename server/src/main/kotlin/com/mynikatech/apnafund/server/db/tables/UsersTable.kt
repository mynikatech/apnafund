package com.mynikatech.apnafund.server.db.tables

import org.jetbrains.exposed.sql.Table

object UsersTable : Table("users") {
    val userId        = integer("userId").autoIncrement()
    val firstName     = varchar("firstName", 255)
    val lastName      = varchar("lastName", 255).nullable()
    val emailId       = varchar("emailId", 255)
    val phoneNumber   = varchar("phoneNumber", 64).nullable()
    val status        = varchar("status", 32).default("ACTIVE")
    val passwordHash  = varchar("passwordHash", 255).nullable()
    val createdDate   = text("createdDate")
    val isPinSet      = bool("isPinSet").default(false)
    val hashPIN       = varchar("hashPIN", 255).nullable()
    val firebaseUserId= varchar("firebaseUserId", 255).nullable()
    val userCode      = varchar("userCode", 64)

    override val primaryKey = PrimaryKey(userId)
}