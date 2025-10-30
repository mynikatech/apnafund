package com.mynikatech.apnafund.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity( tableName="fund_details",
    foreignKeys = [
        ForeignKey(
            entity = Funds::class,
            parentColumns = ["fundId"],
            childColumns = ["fundId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FundDetails(

    @PrimaryKey(autoGenerate = true) val fundDetailsId: Int = 0,
    val fundId: Int,
    val totalExpectedDeposit: Double,
    val totalCurrentDeposit: Double,
    val totalCurrentLateFee: Double,
    val totalCurrentInterestCollected: Double,
    val totalExpectedMaturityAmount: Double,
    val totalCurrAmount: Double
)
