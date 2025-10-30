package com.mynikatech.apnafund.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mynikatech.apnafund.data.model.Deposits
import com.mynikatech.apnafund.data.model.DepositsWithMemberNames
import com.mynikatech.apnafund.data.model.FundDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface DepositsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addDeposits(deposits: Deposits)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAllDeposits(deposits: List<Deposits>)

    @Query("SELECT * from deposits")
    fun getAllDeposits(): Flow<List<Deposits>>

    @Query("SELECT * from deposits WHERE fundId = :fundId")
    fun getAllDepositsforFund(fundId: Int): List<Deposits>

    @Query("""
        SELECT * FROM deposits
        WHERE fundId = :fundId
        AND strftime('%m', depositedDate / 1000, 'unixepoch') = printf('%02d', :month)
        AND strftime('%Y', depositedDate / 1000, 'unixepoch') = :year
    """)
    fun getDepositsForFundMonthYear(fundId: Int, month: Int, year: Int): List<Deposits>

    @Query("""
        SELECT * FROM deposits
        WHERE fundId = :fundId
        AND depositorId = :depositorId
        AND depositMonth = :month
        AND depositYear = :year
        LIMIT 1
    """)
    suspend fun getDepositsForFundDepositorMonthYear( depositorId: Int, fundId: Int, month: String, year: String): Deposits?

    @Query("SELECT * from deposits WHERE fundId = :fundId and depositorId = :userId")
    fun getAllDepositsforFund(fundId: Int, userId: Int): List<Deposits>

    @Query("""
        SELECT d.*, u.firstName, u.lastName, u.userId FROM Users u
        INNER JOIN group_members gm ON u.userId = gm.userId
        INNER JOIN funds f ON gm.groupId = f.groupId
        LEFT JOIN deposits d
        ON u.userId = d.depositorId 
        AND d.fundId = :fundId
        AND d.depositMonth = :month 
        AND d.depositYear = :year
        WHERE f.fundId = :fundId
        order by u.firstName
     """)
    suspend fun getAllDepositsForFundForMonthYear( fundId: Int, month: String, year: String): List<DepositsWithMemberNames>


    @Query("""
        SELECT * FROM deposits
        WHERE fundId = :fundId AND depositorId = :depositorId
        AND depositMonth = :month
        AND depositYear = :year
        LIMIT 1
    """)
    suspend fun getDepositForMember(fundId: Int, depositorId: Int, month: String, year: String): Deposits?

    @Update
    suspend fun updateDeposits(deposits: Deposits)

    @Delete
    suspend fun deleteDeposits(deposits: Deposits)

    @Query("""
        SELECT * FROM deposits
        WHERE fundId = :fundId AND depositorId = :depositorId
    """)
    suspend fun getDepositForMemberForFund(fundId: Int, depositorId: Int): List<Deposits>

    @Query("SELECT * from fund_details WHERE fundId =:fundId")
    suspend fun getFundDetails(fundId: Int): FundDetails

    @Update
    suspend fun updateFundDetails(details: FundDetails)

    @Transaction
    suspend fun saveOrUpdateAllAndFetch(
        deposits: List<Deposits>,
        fundId: Int,
        month: String,
        year: String
    ): List<DepositsWithMemberNames> {
        // Get current FundDetails object
        // calculate totalCurrentDeposit, totalCurrentLateFee,totalExpectedMaturityAmount, totalCurrAmount
        // for Fund and update Fund Details
        val fundDtls = getFundDetails(fundId)
        var totalCurrentDeposit = fundDtls.totalCurrentDeposit
        var totalCurrentLateFee = fundDtls.totalCurrentLateFee
        var totalCurrAmount = fundDtls.totalCurrAmount
        var totalExpectedMaturityAmount = fundDtls.totalExpectedMaturityAmount
        for (dep in deposits) {
            val existing = getDepositForMember(dep.fundId, dep.depositorId, dep.depositMonth, dep.depositYear)
            if (existing != null) {
                updateDeposits(dep.copy(depositId = existing.depositId))
                if(existing.depositAmount < dep.depositAmount){
                    totalCurrentDeposit += (dep.depositAmount - existing.depositAmount)
                    totalCurrAmount += (dep.depositAmount - existing.depositAmount)
                } else if (existing.depositAmount > dep.depositAmount) {
                    totalCurrentDeposit -= (existing.depositAmount - dep.depositAmount)
                    totalCurrAmount -= (existing.depositAmount - dep.depositAmount)
                } else {
                    //do nothing.
                }
                if(existing.lateFee!! < dep.lateFee!!){
                    totalCurrentLateFee += dep.lateFee - existing.lateFee
                    totalExpectedMaturityAmount += dep.lateFee - existing.lateFee
                    totalCurrAmount += dep.lateFee - existing.lateFee
                } else if (existing.lateFee > dep.lateFee) {
                    totalCurrentLateFee -= existing.lateFee - dep.lateFee
                    totalExpectedMaturityAmount -= existing.lateFee - dep.lateFee
                    totalCurrAmount -= existing.lateFee - dep.lateFee
                } else {
                    //do nothing.
                }

            } else {
                addDeposits(dep)
                totalCurrentDeposit += dep.depositAmount
                totalCurrentLateFee += dep.lateFee!!
                totalCurrAmount += dep.depositAmount + dep.lateFee
                totalExpectedMaturityAmount += dep.lateFee

            }
        }
        val updatedFundDetls = FundDetails(
            fundDetailsId = fundDtls.fundDetailsId,
            fundId = fundId,
            totalExpectedDeposit = fundDtls.totalExpectedDeposit,
            totalCurrentDeposit = totalCurrentDeposit,
            totalCurrentLateFee = totalCurrentLateFee,
            totalCurrentInterestCollected = fundDtls.totalCurrentInterestCollected,
            totalExpectedMaturityAmount = totalExpectedMaturityAmount,
            totalCurrAmount = totalCurrAmount
        )
        updateFundDetails(updatedFundDetls)

        return getAllDepositsForFundForMonthYear(fundId, month, year)
    }
}