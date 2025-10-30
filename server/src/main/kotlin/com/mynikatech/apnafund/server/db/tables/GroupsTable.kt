package com.mynikatech.apnafund.server.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object GroupsTable : Table("groups") {
    val groupId = integer("groupId").autoIncrement()
    val groupName = varchar("groupName", 255)
    val moderator   = integer("moderator")
        .references(UsersTable.userId, onDelete = ReferenceOption.CASCADE)
        .nullable()
    val createdDate = text("createdDate")            // keep TEXT to mirror Room
    val description = text("description").nullable()
    val groupCode = varchar("groupCode", 64)
    val status = varchar("status", 32).default("ACTIVE")

    override val primaryKey = PrimaryKey(groupId)
}