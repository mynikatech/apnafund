package com.mynikatech.apnafund.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mynikatech.apnafund.data.model.FundWithDetails
import com.mynikatech.apnafund.data.model.Funds
import com.mynikatech.apnafund.data.model.Groups
import com.mynikatech.apnafund.data.model.UserDetails
import com.mynikatech.apnafund.data.model.UserFundDetails
import com.mynikatech.apnafund.data.model.UserNotifications
import com.mynikatech.apnafund.data.model.UserProfile
import com.mynikatech.apnafund.data.model.UserRoles
import com.mynikatech.apnafund.data.model.Users
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSummaryDao {

    @Query("SELECT * from users")
    fun getAllUsers(): Flow<List<Users>>

    @Query("SELECT * from users")
    suspend fun getUsers(): List<Users>

    @Query("SELECT * from users WHERE userId = :userId")
    suspend fun getUser(userId: Int): Users?

    @Query("""SELECT g.* FROM `groups` g
            INNER JOIN group_members gm 
            ON g.groupId = gm.groupId
            WHERE gm.userId = :userId
            """)
    suspend fun getGroupForUser(userId: Int): List<Groups>?

    @Query("""SELECT f.* FROM funds f 
            INNER JOIN group_members gm ON f.groupId = gm.groupId 
            WHERE gm.userId = :userId""")
    suspend fun getFundsForUser(userId: Int): List<Funds>

    @Query("""SELECT SUM(depositAmount) FROM deposits 
        WHERE depositorId = :userId AND fundId = :fundId""")
    suspend fun getTotalDeposit(userId: Int, fundId: Int): Double?

    @Query("""
    SELECT 
        fd.totalExpectedMaturityAmount / COUNT(fm.userId) AS perMemberExpectedMaturityAmount
    FROM fund_details fd
    JOIN funds f ON fd.fundId = f.fundId
    JOIN fund_members fm ON f.fundId = fm.fundId
    WHERE fd.fundId = :fundId
    GROUP BY fd.totalExpectedMaturityAmount
""")
    suspend fun getPerMemberExpectedMaturityAmount(fundId: Int): Double?

    @Insert
    suspend fun addUserRole(userRoles: UserRoles)

    @Query("""SELECT r.roleCode from user_roles ur
            JOIN roles r ON r.roleId = ur.roleId
            JOIN users u ON u.userId = ur.userId
            WHERE u.userId = :userId
        """)
    suspend fun getAllUserRoles(userId: Int): List<String>

    @Query("SELECT * from user_roles WHERE roleId = :roleId")
    fun getAllUsersForARole(roleId: Int): Flow<List<UserRoles>>

    @Update
    suspend fun updateUserRoles(userRoles: UserRoles)

    @Delete
    suspend fun deleteUserRoles(userRoles: UserRoles)

    @Query("SELECT * from user_notifications where userId = :userId")
    suspend fun getUserNotifications(userId: Int): List<UserNotifications>?


    @Query(
        """SELECT SUM(loanAmount) FROM loans 
        WHERE borrowerId = :userId AND fundId = :fundId
        """
    )
    suspend fun getTotalLoanAmount(userId: Int, fundId: Int): Double

    @Query("""SELECT f.*, fd.*, g.groupName,
            u.firstName as moderatorFirstName, u.lastName as moderatorLastName 
            from funds f
            Left JOIN fund_details fd ON f.fundId = fd.fundId 
            LEFT JOIN 'groups' g ON f.groupId = g.groupId
            LEFT JOIN users u ON u.userId = f.moderator
            WHERE f.fundId = :fundId
        """)
    suspend fun getFundWithDetails(fundId: Int): FundWithDetails

    @Transaction
    suspend fun getUserFundDetails(userId: Int, fundId: Int): UserFundDetails?
    {
        val fundDetails = getFundWithDetails(fundId)
        val totalLoanAmount = getTotalLoanAmount(userId, fundId)
        val totalDeposit = getTotalDeposit(userId, fundId)
        val totalExpMatAmount = getPerMemberExpectedMaturityAmount(fundId)
        if( fundDetails != null) {
            return UserFundDetails(
                fundDetails = fundDetails,
                totalLoanAmount = totalLoanAmount,
                totalDeposit = totalDeposit ?: 0.0,
                userExpMatAmount = totalExpMatAmount ?: 0.0
            )
        } else
            return null
    }


}