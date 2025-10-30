package com.mynikatech.apnafund.server.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object LoansTable : Table(name = "loans") {
    val loanId         = integer("loanId").autoIncrement()
    val loanNumber     = varchar("loanNumber", length = 64)
    val borrowerId     = integer("borrowerId") // Room didn't declare FK; leave as plain INT
    val issuedDate     = text("issuedDate")
    val period         = double("period")
    val loanAmount     = double("loanAmount")
    val maturityDate   = text("maturityDate")
    val rateOfInterest = double("rateOfInterest")
    val status         = varchar("status", length = 32).default("ACTIVE")
    val fundId         = integer("fundId")
        .references(FundsTable.fundId, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(loanId)

    init {
        index(isUnique = false, columns = arrayOf(fundId))
        index(isUnique = false, columns = arrayOf(status))
        index(isUnique = false, columns = arrayOf(loanNumber))
    }
}
