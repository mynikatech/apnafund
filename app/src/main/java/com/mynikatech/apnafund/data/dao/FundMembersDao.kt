package com.mynikatech.apnafund.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mynikatech.apnafund.data.model.FundMemberWithName
import com.mynikatech.apnafund.data.model.FundMembers
import kotlinx.coroutines.flow.Flow

@Dao
interface FundMembersDao {

    @Insert
    suspend fun addFundMember(fundMembers: FundMembers)

    @Query("SELECT * from fund_members WHERE fundId = :fundId ")
    suspend fun getAllMembersofFund(fundId: Int): List<FundMembers>

    @Update
    suspend fun updateFundMember(fundMembers: FundMembers)

    @Delete
    suspend fun deleteFundMember(fundMembers: FundMembers)

    // Get GroupMembers for a given Fund
    @Query(
        """
        SELECT fm.*
        FROM fund_members AS fm
        INNER JOIN funds AS f ON fm.fundId = f.fundId
        WHERE f.fundId = :fundId
    """
    )
    fun getFundMembersForFund(fundId: Int): Flow<List<FundMembers>>

    @Query(
        """SELECT CAST(COUNT(1) AS BIT)
            FROM fund_members
            WHERE userId = :userId
            AND fundId = :fundId"""
    )
    suspend fun checkIfFundMemberAlreadyAdded(userId: Int, fundId: Int): Boolean

    @Query(
        """
        SELECT count(fm.fundMemberId)
        FROM fund_members AS fm
        WHERE fm.fundId= :fundId
    """
    )
    suspend fun getTotMemberNumbersForFund(fundId: Int): Int

    @Transaction
    suspend fun addFundMembers(fundMembers: List<FundMembers>){
        fundMembers.forEach {
            addFundMember(it)
        }

    }

    @Query("SELECT * FROM fund_members WHERE fundId = :fundId")
    suspend fun getMembersOnce(fundId: Int): List<FundMembers>

    @Query("SELECT * FROM fund_members WHERE fundMemberId = :id LIMIT 1")
    suspend fun getMemberOrNull(id: Int): FundMembers?
}