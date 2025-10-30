package com.mynikatech.apnafund.server.db.tables

import org.jetbrains.exposed.sql.Table

object RolesTable : Table("roles") {
    val roleId    = integer("roleId").autoIncrement()
    val roleCode  = varchar("roleCode", 64) // e.g., "MEMBER", "MODERATOR"
    val roleDescription = varchar("roleDescription",64).nullable()
    val status    = varchar("status", 32).default("ACTIVE")
    override val primaryKey = PrimaryKey(roleId)
}