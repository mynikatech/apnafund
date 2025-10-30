package com.mynikatech.apnafund.server.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object LoanDetailsTable : Table(name = "loan_details") {
    val loanDetailsId   = integer("loanDetailsId").autoIncrement()
    val loanId          = integer("loanId")
        .references(LoansTable.loanId, onDelete = ReferenceOption.CASCADE)
    val origPrincipal   = double("origPrincipal")
    val totalInterest   = double("totalInterest")
    val totalAmount     = double("totalAmount")
    val emiInterest     = double("emiInterest")
    val currTotalIntPaid= double("currTotalIntPaid")
    val currPrincipal   = double("currPrincipal")

    override val primaryKey = PrimaryKey(loanDetailsId)

    init {
        index(isUnique = false, columns = arrayOf(loanId))
    }
}
