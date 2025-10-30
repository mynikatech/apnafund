package com.mynikatech.apnafund.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mynikatech.apnafund.data.model.FundDetails
import com.mynikatech.apnafund.data.model.FundWithDetails
import com.mynikatech.apnafund.data.model.Funds
import com.mynikatech.apnafund.data.model.Users
import kotlinx.coroutines.flow.Flow

@Dao
interface FundsDao {

    @Insert
    suspend fun addFund(fund: Funds): Long

    @Insert
    suspend fun insertFundDetails(details: FundDetails)

    @Update
    suspend fun updateFundDetails(details: FundDetails)

    @Query("SELECT * from funds")
    suspend fun getAllFunds(): List<Funds>

    @Query("SELECT * from funds WHERE fundStatus != 'Closed'")
    suspend fun getAllActiveFunds(): List<Funds>

    @Query("SELECT fundId from funds WHERE fundCode == :fundCode")
    suspend fun getFundbyCode(fundCode: String): Int

    @Query("SELECT * from funds WHERE fundStatus != 'Closed' AND groupId = :groupId")
    suspend fun getAllActiveFundsForGroup(groupId: Int): List<Funds>

    @Query("""SELECT f.*, fd.*, g.groupName,
            u.firstName as moderatorFirstName, u.lastName as moderatorLastName 
            from funds f
            INNER JOIN fund_details fd ON f.fundId = fd.fundId
            LEFT JOIN 'groups' g ON f.groupId = g.groupId
            LEFT JOIN users u ON u.userId = f.moderator
        """)
    suspend fun getAllFundsWithDetails(): List<FundWithDetails>

    @Query("""SELECT f.*, fd.*, g.groupName,
            u.firstName as moderatorFirstName, u.lastName as moderatorLastName
            from funds f
            LEFT JOIN fund_details fd ON f.fundId = fd.fundId
            LEFT JOIN 'groups' g ON f.groupId = g.groupId
            LEFT JOIN users u ON u.userId = f.moderator
            WHERE g.groupId = :groupId
        """)
    suspend fun getAllFundsWithDetailsForGroup(groupId: Int): List<FundWithDetails>

    @Query("""SELECT f.*, fd.*, g.groupName,
            u.firstName as moderatorFirstName, u.lastName as moderatorLastName
            from funds f
            LEFT JOIN fund_details fd ON f.fundId = fd.fundId 
            LEFT JOIN 'groups' g ON f.groupId = g.groupId
            LEFT JOIN users u ON u.userId = f.moderator
            WHERE f.fundId = :fundId
        """)
    suspend fun getFundWithDetails(fundId: Int): FundWithDetails

    @Query("SELECT * from funds WHERE fundId =:fundId")
    suspend fun getFund(fundId: Int): Funds

    @Query("SELECT * from fund_details WHERE fundId =:fundId")
    suspend fun getFundDetails(fundId: Int): FundDetails?

    @Query("SELECT * from funds WHERE groupId = :groupId")
    fun getAllFundsforGroup(groupId: Int): Flow<List<Funds>>

    @Update
    suspend fun updateFund(fund: Funds)

    @Delete
    suspend fun deleteFund(fund: Funds)

    @Query("SELECT loanInterestRate from funds WHERE fundId =:fundId")
    suspend fun getRateOfInterestforFund(fundId: Int): Double

    @Transaction
    suspend fun insertFundWithDetails(fund: Funds): Int{
        val fundId = addFund(fund).toInt()
        return fundId
    }

    @Transaction
    suspend fun updateFundWithDetails(fund: Funds,fundDetails: FundDetails) {
        updateFund(fund)
        val detailsWithFundId = fundDetails.copy(totalExpectedDeposit = fundDetails.totalExpectedDeposit,
            totalExpectedMaturityAmount = fundDetails.totalExpectedMaturityAmount)
        updateFundDetails(detailsWithFundId)
    }

    @Query("""
    SELECT 
        fd.totalCurrentDeposit
        + fd.totalCurrentLateFee 
        + fd.totalCurrentInterestCollected 
        - IFNULL(SUM(ld.currPrincipal), 0) AS availableAmount
    FROM fund_details fd
    LEFT JOIN loans l ON fd.fundId = l.fundId AND l.status = 'ACTIVE'
    LEFT JOIN loan_details ld ON l.loanId = ld.loanId
    WHERE fd.fundId = :fundId
    GROUP BY fd.fundId
""")
    suspend fun getAvailableFundAmount(fundId: Int): Double?


    @Query("""
    SELECT u.* FROM Users u
    INNER JOIN group_members gm ON u.userId = gm.userId
    WHERE gm.groupId = :groupId
    AND u.userId NOT IN (
        SELECT userId FROM fund_members WHERE fundId = :fundId
    )
""")
    suspend fun getAvailableFundMembers(groupId: Int, fundId: Int): List<Users>

    @Query("SELECT * from  funds WHERE fundId = :id LIMIT 1")
    suspend fun getFundOrNull(id: Int): Funds?

    /** Stream all funds (filter to ACTIVE if you want). */
    @Query("SELECT * FROM funds")
    fun observeAllFunds(): Flow<List<Funds>>

    /** One-shot read for sync. */
    @Query("SELECT * FROM funds")
    suspend fun getAllFundsOnce(): List<Funds>
}