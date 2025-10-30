package com.mynikatech.apnafund.server.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object FundDetailsTable : Table(name = "fund_details") {
    val fundDetailsId              = integer("fundDetailsId").autoIncrement()
    val fundId                     = integer("fundId")
        .references(FundsTable.fundId, onDelete = ReferenceOption.CASCADE)
    val totalExpectedDeposit       = double("totalExpectedDeposit")
    val totalCurrentDeposit        = double("totalCurrentDeposit")
    val totalCurrentLateFee        = double("totalCurrentLateFee")
    val totalCurrentInterestCollected = double("totalCurrentInterestCollected")
    val totalExpectedMaturityAmount= double("totalExpectedMaturityAmount")
    val totalCurrAmount            = double("totalCurrAmount")

    override val primaryKey = PrimaryKey(fundDetailsId)

    init {
        index(isUnique = false, columns = arrayOf(fundId))
    }
}