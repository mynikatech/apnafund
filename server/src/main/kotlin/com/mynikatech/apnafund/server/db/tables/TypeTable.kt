package com.mynikatech.apnafund.server.db.tables

import org.jetbrains.exposed.sql.Table

// "type" is a PostgreSQL keyword; Exposed will quote identifiers when needed.
// If your DB complains, rename to Table(name = "\"type\"") to force quoting.
object TypeTable : Table(name = "type") {
    val typeId          = integer("typeId").autoIncrement()
    val typeCode        = varchar("typeCode", length = 64)      // unique
    val typeDescription = varchar("typeDescription", length = 255)
    val status          = varchar("status", length = 32).default("ACTIVE")

    override val primaryKey = PrimaryKey(typeId)

    init {
        uniqueIndex("type_typeCode_uq", typeCode)
    }
}
