package com.mynikatech.apnafund.server.funds

import com.mynikatech.apnafund.net.dto.*
import com.mynikatech.apnafund.server.mapper.FundWithDetailsMapper
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.kotlin.BindKotlin
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.jdbi.v3.sqlobject.config.RegisterRowMapper

@RegisterKotlinMapper(FundsDto::class)
@RegisterKotlinMapper(FundDetailsDto::class)
@RegisterKotlinMapper(FundWithDetailsDto::class)
@RegisterKotlinMapper(FundMembersDto::class)
@RegisterKotlinMapper(UsersDto::class)
@RegisterRowMapper(FundWithDetailsMapper::class)
interface FundsSql {

    // ---- Funds (DTO/basic) ----
    @SqlQuery("""SELECT * FROM get_funds()""")
    fun getAllFunds(): List<FundsDto>

    @SqlQuery("""SELECT * FROM get_fund(:id)""")
    fun getFund(@Bind("id") id: Int): List<FundsDto>

    @SqlQuery("""
        SELECT add_fund(
            :fundName, :fundStartDate, :fundMaturityDate, :fundPeriod,
            :depositionFrequency, :moderator, :recurringDepositAmount,
            :fundStatus, :loanInterestRate, :lateFeeRate,
            :monthlyDepDateBy, :groupId, :fundCode
        )
    """)
    fun addFund(@BindKotlin f: FundsDto): Int

    @SqlQuery("""
        SELECT update_fund(
            :id, :fundName, :fundStartDate, :fundMaturityDate, :fundPeriod,
            :depositionFrequency, :moderator, :recurringDepositAmount,
            :fundStatus, :loanInterestRate, :lateFeeRate,
            :monthlyDepDateBy, :groupId, :fundCode
        )
    """)
    fun updateFund(
        @Bind("id") id: Int,
        @Bind("fundName") fundName: String?,
        @Bind("fundStartDate") fundStartDate: String?,
        @Bind("fundMaturityDate") fundMaturityDate: String?,
        @Bind("fundPeriod") fundPeriod: Double?,
        @Bind("depositionFrequency") depositionFrequency: String?,
        @Bind("moderator") moderator: Int?,
        @Bind("recurringDepositAmount") recurringDepositAmount: Double?,
        @Bind("fundStatus") fundStatus: String?,
        @Bind("loanInterestRate") loanInterestRate: Double?,
        @Bind("lateFeeRate") lateFeeRate: Double?,
        @Bind("monthlyDepDateBy") monthlyDepDateBy: Int?,
        @Bind("groupId") groupId: Int?,
        @Bind("fundCode") fundCode: String?
    ): Boolean

    @SqlQuery("""SELECT delete_fund(:id)""")
    fun deleteFund(@Bind("id") id: Int): Boolean

    // ---- Funds (domain queries) ----
    @SqlQuery("""SELECT * FROM get_active_funds()""")
    fun getAllActiveFunds(): List<FundsDto>

    @SqlQuery("""SELECT get_fund_by_code(:code)""")
    fun getFundIdByCode(@Bind("code") fundCode: String): Int

    @SqlQuery("""SELECT * FROM get_active_funds_for_group(:groupId)""")
    fun getAllActiveFundsForGroup(@Bind("groupId") groupId: Int): List<FundsDto>

    @SqlQuery("""SELECT * FROM get_active_funds_for_group_moderator(:groupId, :userId)""")
    fun getAllActiveFundsForGroupAndModerator(@Bind("groupId") groupId: Int, @Bind("userId") userId: Int): List<FundsDto>

    @SqlQuery("""SELECT * FROM get_funds_for_group(:groupId)""")
    fun getAllFundsForGroup(@Bind("groupId") groupId: Int): List<FundsDto>

    // ---- Projections with details ----
    @SqlQuery("""SELECT * FROM get_all_funds_with_details()""")
    fun allWithDetails(): List<FundWithDetailsDto>

    @SqlQuery("""SELECT * FROM get_all_funds_with_details_for_group(:groupId)""")
    fun allWithDetailsForGroup(@Bind("groupId") groupId: Int): List<FundWithDetailsDto>

    @SqlQuery("""SELECT * FROM get_fund_with_details(:fundId)""")
    fun getFundWithDetails(@Bind("fundId") fundId: Int): FundWithDetailsDto

    // ---- Interest ----
    @SqlQuery("""SELECT get_rate_of_interest_for_fund(:fundId)""")
    fun getRateOfInterestForFund(@Bind("fundId") fundId: Int): Double?

    // ---- Members ----
    @SqlQuery("""SELECT * FROM get_fund_members(:fundId)""")
    fun getFundMembers(@Bind("fundId") fundId: Int): List<FundMembersDto>

    @SqlQuery("""SELECT add_fund_member(:userId, :fundId, :joiningDate)""")
    fun addFundMember(@BindKotlin m: FundMembersDto): Int

    @SqlQuery("""SELECT add_fund_members_batch(CAST(:itemsJson AS jsonb))""")
    fun addFundMembersBatch(@Bind("itemsJson") itemsJson: String): List<Int>

    @SqlQuery("""SELECT delete_fund_member(:fundMemberId)""")
    fun removeFundMember(@Bind("fundMemberId") fundMemberId: Int): Boolean

    // ---- Details ----
    @SqlQuery("""SELECT * FROM get_fund_details(:fundId)""")
    fun getFundDetails(@Bind("fundId") fundId: Int): List<FundDetailsDto>

    @SqlUpdate("""CALL insert_fund_details(:fundId, :totalExpectedDeposit, :totalCurrentDeposit, :totalCurrentLateFee, :totalCurrentInterestCollected, :totalExpectedMaturityAmount, :totalCurrAmount)""")
    fun insertFundDetails(@BindKotlin d: FundDetailsDto)

    @SqlUpdate("""CALL update_fund_details(:fundDetailsId, :fundId, :totalExpectedDeposit, :totalCurrentDeposit, :totalCurrentLateFee, :totalCurrentInterestCollected, :totalExpectedMaturityAmount, :totalCurrAmount)""")
    fun updateFundDetails(@BindKotlin d: FundDetailsDto)

    @SqlQuery("""SELECT upsert_fund_details(:fundId, :totalExpectedDeposit, :totalCurrentDeposit, :totalCurrentLateFee, :totalCurrentInterestCollected, :totalExpectedMaturityAmount, :totalCurrAmount)""")
    fun upsertDetails(@BindKotlin d: FundDetailsDto): Int

    // ---- Available amount / eligible members ----
    @SqlQuery("""SELECT get_available_fund_amount(:fundId)""")
    fun availableAmount(@Bind("fundId") fundId: Int): Double?

    // ---- Available amount / eligible members ----
    @SqlQuery("""SELECT * FROM get_fund_availability(:fundId)""")
    fun getFundAvailability(@Bind("fundId") fundId: Int): FundAvailabilityDto?

    @SqlQuery("""SELECT * FROM get_available_fund_members(:groupId, :fundId)""")
    fun availableMembers(@Bind("groupId") groupId: Int, @Bind("fundId") fundId: Int): List<UsersDto>

    @SqlQuery("""SELECT * FROM get_fund_members_with_names_for_fund(:fundId)""")
    fun getFundMembersWithNamesForFund(@Bind("fundId") fundId: Int): List<FundMemberWithNameDto>

    @SqlQuery("""SELECT check_fund_member_exists(:userId, :fundId)""")
    fun checkIfFundMemberAlreadyAdded(
        @Bind("userId") userId: Int,
        @Bind("fundId") fundId: Int
    ): Boolean

    @SqlQuery("""SELECT add_fund_with_details(CAST(:fund AS jsonb), CAST(:details AS jsonb))""")
    fun addFundWithDetails(@Bind("fund") fundJson: String, @Bind("details") detailsJson: String): Int

    @SqlQuery("""
    SELECT close_fund(
        :fundId,
        :closedBy,
        :reason
    )
""")
    fun closeFund(
        @Bind("fundId") fundId: Int,
        @Bind("closedBy") closedBy: Int,
        @Bind("reason") reason: String
    ): Boolean


}
