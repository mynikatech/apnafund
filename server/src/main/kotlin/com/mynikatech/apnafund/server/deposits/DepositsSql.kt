package com.mynikatech.apnafund.server.deposits

import com.mynikatech.apnafund.net.dto.DepositsDto
import com.mynikatech.apnafund.net.dto.DepositsWithMemberNamesDto
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.kotlin.BindKotlin
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlBatch
import org.jdbi.v3.sqlobject.statement.SqlQuery

@RegisterKotlinMapper(DepositsDto::class)
@RegisterKotlinMapper(DepositsWithMemberNamesDto::class)
interface DepositsSql {

    @SqlQuery("""SELECT * FROM get_deposits_with_names_for_fund(:fundId)""")
    fun getDepositsWithNamesForFund(@Bind("fundId") fundId: Int): List<DepositsWithMemberNamesDto>

    @SqlQuery("""SELECT * FROM get_deposits_with_names_for_fund_month_year(:fundId, :month, :year)""")
    fun getDepositsWithNamesForFundMonthYear(
        @Bind("fundId") fundId: Int,
        @Bind("month") month: String,
        @Bind("year") year: String
    ): List<DepositsWithMemberNamesDto>

    @SqlQuery("""SELECT * FROM get_deposit(:id)""")
    fun getDeposit(@Bind("id") id: Int): List<DepositsDto>

    @SqlQuery("""SELECT * FROM get_all_deposits()""")
    fun getAllDeposits(): List<DepositsDto>

    @SqlQuery("""SELECT * FROM get_deposits_for_fund(:fundId)""")
    fun getDepositsForFund(@Bind("fundId") fundId: Int): List<DepositsDto>

    @SqlQuery("""SELECT * FROM get_deposits_for_fund_month_year(:fundId, :month, :year)""")
    fun getDepositsForFundMonthYear(
        @Bind("fundId") fundId: Int,
        @Bind("month") month: String,
        @Bind("year") year: String
    ): List<DepositsDto>

    @SqlQuery("""SELECT * FROM get_deposits_for_fund_depositor_month_year(:depositorId, :fundId, :month, :year)""")
    fun getDepositsForFundDepositorMonthYear(
        @Bind("depositorId") depositorId: Int,
        @Bind("fundId") fundId: Int,
        @Bind("month") month: String,
        @Bind("year") year: String
    ): List<DepositsDto>

    @SqlQuery("""SELECT * FROM get_all_deposits_for_fund_for_month_year(:fundId, :month, :year)""")
    fun getAllDepositsForFundForMonthYear(
        @Bind("fundId") fundId: Int,
        @Bind("month") month: String,
        @Bind("year") year: String
    ): List<DepositsWithMemberNamesDto>

    @SqlQuery("""SELECT * FROM get_deposit_for_member(:fundId, :depositorId, :month, :year)""")
    fun getDepositForMember(
        @Bind("fundId") fundId: Int,
        @Bind("depositorId") depositorId: Int,
        @Bind("month") month: String,
        @Bind("year") year: String
    ): List<DepositsDto>

    @SqlQuery("""SELECT * FROM get_deposit_for_member_for_fund(:fundId, :depositorId)""")
    fun getDepositForMemberForFund(
        @Bind("fundId") fundId: Int,
        @Bind("depositorId") depositorId: Int
    ): List<DepositsDto>

    @SqlQuery(
        """
        SELECT add_deposit(
            :depositorId,
            :fundId,
            CAST(:depositedDate AS date),
            :depositAmount,
            :depositMonth,
            :depositYear,
            :lateFee
        )
    """
    )
    fun addDeposit(@BindKotlin d: DepositsDto): Int

    @SqlQuery(
        """
        SELECT update_deposit(
            :id,
            :depositorId,
            :fundId,
            CAST(:depositedDate AS date),
            :depositAmount,
            :depositMonth,
            :depositYear,
            :lateFee
        )
    """
    )
    fun updateDeposit(
        @Bind("id") id: Int,
        @Bind("depositorId") depositorId: Int?,
        @Bind("fundId") fundId: Int?,
        @Bind("depositedDate") depositedDate: String?,
        @Bind("depositAmount") depositAmount: Double?,
        @Bind("depositMonth") depositMonth: String?,
        @Bind("depositYear") depositYear: String?,
        @Bind("lateFee") lateFee: Double?
    ): Boolean

    @SqlQuery("""SELECT delete_deposit(:id)""")
    fun deleteDeposit(@Bind("id") id: Int): Boolean


        @SqlBatch(
            """
        WITH upserted AS (
          INSERT INTO "deposits"(
            "depositorId","fundId","depositedDate",
            "depositAmount","depositMonth","depositYear","lateFee"
          )
          VALUES (
            :depositorId,
            :fundId,
            :depositedDate,
            :depositAmount,
            :depositMonth,
            :depositYear,
            COALESCE(:lateFee, 0)
          )
          ON CONFLICT ("fundId","depositorId","depositMonth","depositYear")
          DO UPDATE SET
            "depositedDate" = EXCLUDED."depositedDate",
            "depositAmount" = EXCLUDED."depositAmount",
            "lateFee"       = EXCLUDED."lateFee"
          RETURNING
            "depositId","depositorId","fundId","depositedDate",
            "depositAmount","depositMonth","depositYear","lateFee"
        )
        SELECT
          u."depositId"                                 AS "depositId",
          u."depositorId"                               AS "depositorId",
          u."depositedDate"                             AS "depositedDate",
          u."depositAmount"                             AS "depositAmount",
          u."depositMonth"                              AS "depositMonth",
          u."depositYear"                               AS "depositYear",
          u."lateFee"                                   AS "lateFee",
          u."fundId"                                    AS "fundId",
          m."firstName"                                 AS "firstName",
          m."lastName"                                  AS "lastName",
          m."userId"                                    AS "userId"
        FROM upserted u
        JOIN "users" m ON m."userId" = u."depositorId"
    """
        )
        @org.jdbi.v3.sqlobject.statement.GetGeneratedKeys
        fun upsertDeposits(
            @BindKotlin items: List<DepositsDto>
        ): List<DepositsWithMemberNamesDto>

    @SqlQuery("""SELECT * FROM save_or_update_all_deposits_and_fetch(:fundId, :month, :year, :rows::jsonb)""")
    fun saveOrUpdateAllDepositsAndFetch(
        @Bind("fundId") fundId: Int,
        @Bind("month") month: String,
        @Bind("year") year: String,
        @Bind("rows") rowsJson: String
    ): List<DepositsWithMemberNamesDto>

}
