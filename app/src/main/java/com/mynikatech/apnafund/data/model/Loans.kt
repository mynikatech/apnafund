package com.mynikatech.apnafund.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.mynikatech.apnafund.constants.ApnaBankConstants

@Entity(tableName= "loans",
    foreignKeys = [
        ForeignKey(
            entity = Funds::class,
            parentColumns = ["fundId"],
            childColumns = ["fundId"],
            onDelete = ForeignKey.CASCADE
        )
    ]

)
data class Loans(

    @PrimaryKey(autoGenerate = true) val loanId: Int = 0,
    val loanNumber: String,
    val borrowerId: Int,
    val issuedDate: String,
    val period: Double,
    val loanAmount: Double,
    val maturityDate: String,
    val rateOfInterest: Double,
    val hasVariableInterestRate: Boolean = false,
    val revisedLoanInterestRate: Double? = null,
    val interestRateRevisionAfterMonths: Int? = null,
    val status: String = ApnaBankConstants.STATUS_ACTIVE,
    val fundId: Int,
    val workflowStatus: String,
    val closedDate: String? = null,
    val closureType: String? = null,
    val closureSource: String? = null
)
