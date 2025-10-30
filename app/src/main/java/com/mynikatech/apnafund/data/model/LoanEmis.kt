package com.mynikatech.apnafund.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName="loan_emi",
    foreignKeys = [
        ForeignKey(
            entity = Loans::class,
            parentColumns = ["loanId"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE
        )
    ]

)
data class LoanEmis(

    @PrimaryKey(autoGenerate = true) val loanEmiId: Int = 0,
    val loanId: Int,
    val emiMonth: String,
    val emiYear: String,
    val emiDepositedDate: String,
    val emiDepositedAmount: Double,
    val prepaymentAmount: Double,
    val lateFee: Double
)
