package com.mynikatech.apnafund.server.db.tables

import org.jetbrains.exposed.sql.Table

object PrivilegeTable : Table(name = "privilege") {
    val privilegeId          = integer("privilegeId").autoIncrement()
    val privilegeCode        = varchar("privilegeCode", length = 64)
    val privilegeDescription = varchar("privilegeDescription", length = 255)
    val status               = varchar("status", length = 32).default("ACTIVE")

    override val primaryKey = PrimaryKey(privilegeId)

    init {
        uniqueIndex("privilege_code_uq", privilegeCode)
    }
}
