package com.mynikatech.apnafund.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mynikatech.apnafund.data.model.LoanDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDetailsDao {

    @Insert
    suspend fun addLoanDetails(loanDetails: LoanDetails)

    @Query("SELECT * from loan_details")
    fun getAllLoanDetails(): Flow<List<LoanDetails>>

    @Query("SELECT * from loan_details WHERE loanId = :loanId")
    fun getLoanDetailsforLoan(loanId: Int): Flow<List<LoanDetails>>

    @Update
    suspend fun updateLoanDetails(loanDetails: LoanDetails)

    @Delete
    suspend fun deleteLoanDetails(loanDetails: LoanDetails)


}