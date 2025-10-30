package com.mynikatech.apnafund.server.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object FundsTable : Table(name = "funds") {
    val fundId                = integer("fundId").autoIncrement()
    val fundName              = varchar("fundName", length = 255)
    val fundStartDate         = text("fundStartDate")         // keep as TEXT to match Room String
    val fundMaturityDate      = text("fundMaturityDate")
    val fundPeriod            = double("fundPeriod")
    val depositionFrequency   = varchar("depositionFrequency", length = 64)
    val moderator             = integer("moderator")          // Room has no FK; keep as INT
    val recurringDepositAmount= double("recurringDepositAmount")
    val fundStatus            = varchar("fundStatus", length = 32).default("ACTIVE")
    val loanInterestRate      = double("loanInterestRate")
    val lateFeeRate           = double("lateFeeRate")
    val monthlyDepDateBy      = integer("monthlyDepDateBy")
    val groupId               = integer("groupId")
        .references(GroupsTable.groupId, onDelete = ReferenceOption.CASCADE)
    val fundCode              = varchar("fundCode", length = 64)

    override val primaryKey = PrimaryKey(fundId)

    init {
        index(isUnique = true, columns = arrayOf(fundCode))
        index(isUnique = false, columns = arrayOf(groupId))
        index(isUnique = false, columns = arrayOf(fundStatus))
    }
}
