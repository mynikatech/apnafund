package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.net.dto.DepositsDto
import com.mynikatech.apnafund.net.dto.DepositsWithMemberNamesDto
import com.mynikatech.apnafund.net.dto.SaveOrUpdateDepositRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DepositsApiKtor(
    private val clientProvider: () -> io.ktor.client.HttpClient = { HttpClientProvider.client }
) : DepositsApi {

    private val client get() = clientProvider()

    override suspend fun getByFund(fundId: Int): List<DepositsWithMemberNamesDto> =
        client.get("/deposits/get/with-names/fund/$fundId")
            .unwrap<List<DepositsWithMemberNamesDto>>()

    override suspend fun getByFundMonthYear(
        fundId: Int,
        month: String,
        year: String
    ): List<DepositsWithMemberNamesDto> =
        client.get("/deposits/get/with-names/fund/$fundId/$month/$year")
            .unwrap<List<DepositsWithMemberNamesDto>>()

    override suspend fun get(id: Int): DepositsDto? =
        client.get("/deposits/get/$id").unwrap<DepositsDto>()

    override fun getAllDeposits(): Flow<List<DepositsDto>> = flow {
        val list: List<DepositsDto> = client.get("/deposits/get/all").unwrap<List<DepositsDto>>()
        emit(list)
    }

    override suspend fun getAllDepositsforFund(fundId: Int): List<DepositsDto> =
        client.get("/deposits/get/all/fund").unwrap<List<DepositsDto>>()

    override suspend fun getDepositsForFundMonthYear(
        fundId: Int,
        month: Int,
        year: Int
    ): List<DepositsDto> =
        client.get("/deposits/get/$fundId/$month/$year").unwrap<List<DepositsDto>>()

    override suspend fun getDepositsForFundDepositorMonthYear(
        depositorId: Int,
        fundId: Int,
        month: String,
        year: String
    ): DepositsDto? =
        client.get("/deposits/get/one/fund/$fundId/depositor/$depositorId/$month/$year")
            .unwrap<DepositsDto>()

    override suspend fun getAllDepositsForFundForMonthYear(
        fundId: Int,
        month: String,
        year: String
    ): List<DepositsWithMemberNamesDto> =
        client.get("/deposits/get/with-names/full/fund/$fundId/$month/$year")
            .unwrap<List<DepositsWithMemberNamesDto>>()

    override suspend fun addDeposits(dto: DepositsDto): Int =
        client.post("/deposits/add") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.unwrap<Int>()

    override suspend fun addAllDeposits(deposits: List<DepositsDto>) {
        client.post("/deposits/add/bulk") {
            contentType(ContentType.Application.Json)
            setBody(deposits)
        }.unwrap<List<Int>>()
    }

    // ---- Updates / deletes ----

    override suspend fun updateDeposits(id: Int, dto: DepositsDto): Boolean =
        client.put("/deposits/update/$id") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.unwrap<Boolean>()

    override suspend fun deleteDeposits(id: Int): Boolean =
        client.delete("/deposits/delete/$id").unwrap<Boolean>()

    // ---- Targeted lookups (domain) ----

    override suspend fun getDepositForMember(
        fundId: Int,
        depositorId: Int,
        month: String,
        year: String
    ): DepositsDto? =
        client.get("/deposits/get/member") {
            parameter("fundId", fundId)
            parameter("depositorId", depositorId)
            parameter("month", month)
            parameter("year", year)
        }.unwrap<DepositsDto>()

    override suspend fun getDepositForMemberForFund(
        fundId: Int,
        depositorId: Int
    ): List<DepositsDto> =
        client.get("/deposits/get/member/fund") {
            parameter("fundId", fundId)
            parameter("depositorId", depositorId)
        }.unwrap<List<DepositsDto>>()


    // ---- Save-or-update batch, then fetch (domain) ----

    override suspend fun saveOrUpdateAllAndFetch(
        deposits: List<DepositsDto>,
        fundId: Int,
        month: String,
        year: String
    ): List<DepositsWithMemberNamesDto> =
        client.post("/deposits/save-or-update/fetch") {
            val req = SaveOrUpdateDepositRequest(
                deposits = deposits,
                fundId = fundId,
                month = month,
                year = year
            )
            contentType(ContentType.Application.Json)
            setBody(req)
        }.unwrap<List<DepositsWithMemberNamesDto>>()
}
