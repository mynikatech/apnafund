package com.mynikatech.apnafund.server.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object LoanEmisTable : Table(name = "loan_emi") {
    val loanEmiId           = integer("loanEmiId").autoIncrement()
    val loanId              = integer("loanId")
        .references(LoansTable.loanId, onDelete = ReferenceOption.CASCADE)
    val emiMonth            = varchar("emiMonth", length = 16)
    val emiYear             = varchar("emiYear", length = 16)
    val emiDepositedDate    = text("emiDepositedDate")
    val emiDepositedAmount  = double("emiDepositedAmount")
    val prepaymentAmount    = double("prepaymentAmount")
    val lateFee             = double("lateFee")

    override val primaryKey = PrimaryKey(loanEmiId)

    init {
        index(isUnique = false, columns = arrayOf(loanId))
    }
}
