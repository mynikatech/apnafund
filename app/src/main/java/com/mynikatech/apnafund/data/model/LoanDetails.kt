package com.mynikatech.apnafund.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "loan_details",
    foreignKeys = [
        ForeignKey(
            entity = Loans::class,
            parentColumns = ["loanId"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE
        )
    ]

)
data class LoanDetails(

    @PrimaryKey(autoGenerate = true) val loanDetailsId: Int = 0,
    val loanId: Int,
    val origPrincipal: Double,
    val totalInterest: Double,
    val totalAmount: Double,
    val emiInterest: Double,
    val currTotalIntPaid: Double,
    val currPrincipal: Double
)
