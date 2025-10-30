package com.mynikatech.apnafund.server.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object FundMembersTable : Table(name = "fund_members") {
    val fundMemberId = integer("fundMemberId").autoIncrement()
    val userId       = integer("userId")
        .references(UsersTable.userId, onDelete = ReferenceOption.CASCADE)
    val fundId       = integer("fundId")
        .references(FundsTable.fundId, onDelete = ReferenceOption.CASCADE)
    val joiningDate  = text("joiningDate")

    override val primaryKey = PrimaryKey(fundMemberId)

    init {
        // Prevent duplicate membership
        index(isUnique = true, columns = arrayOf(userId, fundId))
        index(isUnique = false, columns = arrayOf(fundId))
        index(isUnique = false, columns = arrayOf(userId))
    }
}