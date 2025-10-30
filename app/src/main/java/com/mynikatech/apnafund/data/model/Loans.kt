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
    val status: String = ApnaBankConstants.STATUS_ACTIVE,
    val fundId: Int
)
