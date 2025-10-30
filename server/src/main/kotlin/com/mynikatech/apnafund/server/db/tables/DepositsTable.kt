package com.mynikatech.apnafund.server.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object DepositsTable : Table(name = "deposits") {
    val depositId      = integer("depositId").autoIncrement()
    val depositorId    = integer("depositorId")         // no FK in Room; keep as INT
    val depositedDate  = text("depositedDate")
    val depositAmount  = double("depositAmount")
    val depositMonth   = varchar("depositMonth", length = 16)
    val depositYear    = varchar("depositYear",  length = 16)
    val fundId         = integer("fundId")
        .references(FundsTable.fundId, onDelete = ReferenceOption.CASCADE)
    val lateFee        = double("lateFee").nullable()

    override val primaryKey = PrimaryKey(depositId)

    init {
        index(isUnique = false, columns = arrayOf(fundId))
        index(isUnique = false, columns = arrayOf(depositorId))

    }
}
