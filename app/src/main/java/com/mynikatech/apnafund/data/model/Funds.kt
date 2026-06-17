package com.mynikatech.apnafund.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.mynikatech.apnafund.constants.ApnaBankConstants

@Entity( tableName="funds",
        foreignKeys = [
            ForeignKey(
                entity = Groups::class,
                parentColumns = ["groupId"],
                childColumns = ["groupId"],
                onDelete = ForeignKey.CASCADE
         )
    ]
)
data class Funds(

    @PrimaryKey(autoGenerate = true) val fundId: Int = 0,
    val fundName: String,
    val fundStartDate: String,
    val fundMaturityDate: String,
    val fundPeriod: Double,
    val depositionFrequency: String,
    val moderator: Int,
    val recurringDepositAmount: Double,
    val fundStatus: String = ApnaBankConstants.STATUS_ACTIVE,
    val loanInterestRate: Double,
    val hasVariableInterestRate: Boolean = false,
    val revisedLoanInterestRate: Double? = null,
    val interestRateRevisionAfterMonths: Int? = null,
    val lateFeeRate: Double,
    val monthlyDepDateBy: Int,
    val groupId: Int,
    val fundCode: String
)
