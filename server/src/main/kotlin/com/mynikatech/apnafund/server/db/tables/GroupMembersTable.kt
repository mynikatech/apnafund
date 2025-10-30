package com.mynikatech.apnafund.server.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object GroupMembersTable : Table("group_members") {
    val groupMemberId = integer("groupMemberId").autoIncrement()
    val userId      = integer("userId").references(UsersTable.userId, onDelete = ReferenceOption.CASCADE)
    val groupId     = integer("groupId").references(GroupsTable.groupId, onDelete = ReferenceOption.CASCADE)
    val joiningDate = text("joiningDate")

    override val primaryKey = PrimaryKey(groupMemberId)
}