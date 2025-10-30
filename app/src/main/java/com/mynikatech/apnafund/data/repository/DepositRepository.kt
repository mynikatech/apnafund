package com.mynikatech.apnafund.data.repository

import com.mynikatech.apnafund.data.dao.DepositsDao
import com.mynikatech.apnafund.data.mappers.toDto
import com.mynikatech.apnafund.data.mappers.toEntity
import com.mynikatech.apnafund.data.model.Deposits
import com.mynikatech.apnafund.data.model.DepositsWithMemberNames
import com.mynikatech.apnafund.net.DepositsApi
import com.mynikatech.apnafund.util.ApnaBankDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DepositRepository(
    private val depositDao: DepositsDao,
    private val api: DepositsApi
) {


    suspend fun getDepositsForFundMonthYear(fundId: Int, month: Int, year: Int): List<Deposits> {
        val (m, y) = ApnaBankDate.normalizeMonthYear(month, year)
        return api.getDepositsForFundMonthYear(fundId, m.toInt(), y.toInt()).toEntity()
    }

    suspend fun getDepositForFundDepositorMonthYear(
        depositorId: Int, fundId: Int, month: Int, year: Int
    ): Deposits? {
        val (m, y) = ApnaBankDate.normalizeMonthYear(month, year)
        return api.getDepositsForFundDepositorMonthYear(depositorId, fundId, m, y)?.toEntity()
    }

    suspend fun getAllDepositForFundForMonthYear(
        fundId: Int, month: Int, year: Int
    ): List<DepositsWithMemberNames> {
        val (m, y) = ApnaBankDate.normalizeMonthYear(month, year)
        return api.getAllDepositsForFundForMonthYear(fundId, m, y).toEntity()
    }

    suspend fun getAllDepositsforFund(fundId: Int): List<Deposits> =
        api.getAllDepositsforFund(fundId).toEntity()

    /** Create remotely, then cache. */
    suspend fun saveDeposit(deposit: Deposits) = withContext(Dispatchers.IO) {
        val id = api.addDeposits(deposit.toDto())
        //depositDao.addDeposits(deposit.copy(depositId = id))
    }

    /** Bulk create/update remotely, then refresh view for month/year. */
    suspend fun saveAllDeposits(deposits: List<Deposits>, fundId: Int, month: Int, year: Int) =
        withContext(Dispatchers.IO) {
            // push each (upsert heuristic: if id==0 -> create, else update)
            deposits.forEach { d ->
                if (d.depositId == 0) {
                    val id = api.addDeposits(d.toDto())
                    //depositDao.addDeposits(d.copy(depositId = id))
                } else {
                    val ok = api.updateDeposits(d.depositId, d.toDto())
                    //if (ok) depositDao.updateDeposits(d)
                }
            }

        }

    /** Update remotely, then cache. */
    suspend fun updateDeposit(deposit: Deposits) = withContext(Dispatchers.IO) {
        require(deposit.depositId != 0) { "depositId required for update" }
        val ok = api.updateDeposits(deposit.depositId, deposit.toDto())
        //if (ok) depositDao.updateDeposits(deposit)
    }

    suspend fun getDepositForMember(
        fundId: Int, depositorId: Int, depositMonth: Int, depositYear: Int
    ): Deposits? {
        val (m, y) = ApnaBankDate.normalizeMonthYear(depositMonth, depositYear)
        return api.getDepositForMember(fundId, depositorId, m, y)?.toEntity()
    }

    suspend fun isDeposited(depositorId: Int, fundId: Int, month: Int, year: Int): Boolean {
        val (m, y) = ApnaBankDate.normalizeMonthYear(month, year)
        return api.getDepositsForFundDepositorMonthYear(depositorId, fundId, m, y) != null
    }

    suspend fun getDepositForFundAndMember(fundId: Int, userId: Int): List<Deposits> =
        api.getDepositForMemberForFund(fundId, userId).toEntity()

    /** Local batch save + fetch view (kept for existing UI call sites). */
    suspend fun saveOrUpdateAllAndFetch(
        deposits: List<Deposits>, fundId: Int, month: String, year: String
    ): List<DepositsWithMemberNames> =
        api.saveOrUpdateAllAndFetch(deposits.toDto(), fundId, month, year).toEntity()
}
