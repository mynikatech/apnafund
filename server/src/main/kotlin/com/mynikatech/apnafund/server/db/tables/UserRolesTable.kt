package com.mynikatech.apnafund.server.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object UserRolesTable : Table("user_roles") {
    val userRoleId = integer("userRoleId").autoIncrement()
    val userId     = integer("userId").references(UsersTable.userId, onDelete = ReferenceOption.CASCADE)
    val roleId     = integer("roleId").references(RolesTable.roleId, onDelete = ReferenceOption.CASCADE)
    val status     = varchar("status", 32).default("ACTIVE")

    override val primaryKey = PrimaryKey(userRoleId)
}