package com.mynikatech.apnafund.net

import android.util.Log
import com.mynikatech.apnafund.net.dto.AddFundWithDetailsRequest
import com.mynikatech.apnafund.net.dto.CloseFundRequest
import com.mynikatech.apnafund.net.dto.FundAvailabilityDto
import com.mynikatech.apnafund.net.dto.FundDetailsDto
import com.mynikatech.apnafund.net.dto.FundMemberWithNameDto
import com.mynikatech.apnafund.net.dto.FundsDto
import com.mynikatech.apnafund.net.dto.FundMembersDto
import com.mynikatech.apnafund.net.dto.FundWithDetailsDto
import com.mynikatech.apnafund.net.dto.UsersDto
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FundsApiKtor(
    private val clientProvider: () -> io.ktor.client.HttpClient = { HttpClientProvider.client }
) : FundsApi {

    private val client get() = clientProvider()

    // -------- Funds (DTO/basic) --------

    override suspend fun getAllFunds(): List<FundsDto> =
        client.get("/funds/get/all").unwrap<List<FundsDto>>()

    override suspend fun getFund(id: Int): FundsDto? =
        client.get("/funds/get/$id").unwrap<FundsDto>()

    override suspend fun addFund(dto: FundsDto): Int =
        client.post("/funds/add") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.unwrap<Int>()

    override suspend fun updateFund(id: Int, dto: FundsDto): Boolean =
        client.put("/funds/update/$id") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.unwrap<Boolean>()

    override suspend fun deleteFund(id: Int): Boolean =
        client.delete("/funds/delete/$id").unwrap<Boolean>()

    // -------- Funds (domain models / queries) --------

    override suspend fun getAllActiveFunds(): List<FundsDto> =
        client.get("/funds/get/active").unwrap<List<FundsDto>>()

    override suspend fun getFundbyCode(fundCode: String): Int =
        client.get("/funds/get/by-code") {
            parameter("fundCode", fundCode)
        }.unwrap<Int>()

    override suspend fun getAllActiveFundsForGroup(groupId: Int): List<FundsDto> =
        client.get("/funds/get/active/group/$groupId").unwrap<List<FundsDto>>()

    override suspend fun getAllFundsWithDetails(): List<FundWithDetailsDto> =
        client.get("/funds/get/with-details/all").unwrap<List<FundWithDetailsDto>>()

    override suspend fun getAllFundsWithDetailsForGroup(
        groupId: Int
    ): List<FundWithDetailsDto> {

        val start = System.currentTimeMillis()

        return try {
            val result = client
                .get("/funds/get/with-details/group/$groupId")
                .unwrap<List<FundWithDetailsDto>>()

            val end = System.currentTimeMillis()
            println("CLIENT SUCCESS TIME: ${end - start} ms")

            result

        } catch (e: io.ktor.client.plugins.HttpRequestTimeoutException) {

            val end = System.currentTimeMillis()
            println("CLIENT TIMEOUT after ${end - start} ms")

            emptyList() // or return cached / fallback

        } catch (e: Exception) {

            val end = System.currentTimeMillis()
            println("CLIENT ERROR after ${end - start} ms: ${e.message}")

            emptyList()
        }
    }
    override suspend fun getFundWithDetails(fundId: Int): FundWithDetailsDto =
        client.get("/funds/get/with-details/$fundId").unwrap<FundWithDetailsDto>()

    override fun getAllFundsforGroup(groupId: Int): Flow<List<FundsDto>> = flow {
        val list: List<FundsDto> = client.get("/funds/get/group/$groupId").unwrap<List<FundsDto>>()
        emit(list)
    }

    override suspend fun getRateOfInterestforFund(fundId: Int): Double =
        client.get("/funds/roi/get/$fundId")
            .unwrap<Double>()

    // -------- Members --------

    override suspend fun getAllFundMembers(fundId: Int): List<FundMembersDto> =
        client.get("/funds/members/get/$fundId").unwrap<List<FundMembersDto>>()

    override suspend fun addFundMember(dto: FundMembersDto): Int =
        client.post("/funds/members/add/one") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.unwrap<Int>()

    override suspend fun addFundMembers(fundMembers: List<FundMembersDto>): List<Int> =
        client.post("/funds/members/add/batch") {
            contentType(ContentType.Application.Json)
            setBody(fundMembers)
        }.unwrap<List<Int>>()

    override suspend fun removeFundMember(fundMemberId: Int): Boolean =
        client.delete("/funds/members/delete/$fundMemberId").unwrap<Boolean>()

    // -------- Details --------

    override suspend fun getFundDetails(fundId: Int): FundDetailsDto? =
        client.get("/funds/details/get/$fundId").unwrap<FundDetailsDto>()

    override suspend fun insertFundDetails(details: FundDetailsDto) {
        client.post("/funds/details/insert") {
            contentType(ContentType.Application.Json)
            setBody(details)
        }.body<Unit>()
    }

    override suspend fun updateFundDetails(details: FundDetailsDto) {
        client.put("/funds/details/update") {
            contentType(ContentType.Application.Json)
            setBody(details)
        }.body<Unit>()
    }

    override suspend fun upsertDetails(dto: FundDetailsDto): Int =
        client.post("/funds/details/upsert") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.unwrap<Int>()

    override suspend fun getAvailableFundAmount(fundId: Int): Double? {
            return try {
                client.get("/funds/available-amount/get/$fundId")
                    .unwrap<Double>()
            } catch (e: Exception) {
                Log.e("FundsApi", "Error fetching fund availability", e)
                null
            }
        }

    override suspend fun getTotalAvailableFundAmount(fundId: Int): FundAvailabilityDto? {
        return try {
            client.get("/funds/available-amounts/get/$fundId")
                .unwrap<FundAvailabilityDto>()
        } catch (e: Exception) {
            Log.e("FundsApi", "Error fetching fund availability", e)
            null
        }
    }

    override suspend fun getAvailableFundMembers(groupId: Int, fundId: Int): List<UsersDto> =
        client.get("/funds/available-members/get") {
            parameter("groupId", groupId)
            parameter("fundId", fundId)
        }.unwrap<List<UsersDto>>()

    override suspend fun updateFundWithDetails(fund: FundsDto, fundDetails: FundDetailsDto) {
        client.put("/funds/update/with-details") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("fund" to fund, "details" to fundDetails))
        }.body<Unit>()
    }

    override suspend fun getFundMembersWithNamesForFund(fundId: Int): List<FundMemberWithNameDto> =
        client.get("/funds/members/get/with-names/$fundId").unwrap<List<FundMemberWithNameDto>>()

    override suspend fun checkIfFundMemberAlreadyAdded(userId: Int, fundId: Int): Boolean =
        client.get("/funds/members/check/$fundId") {
            parameter("userId", userId)
        }.unwrap<Boolean>()

    override suspend fun addFundWithDetails(
        fund: FundsDto,
        details: FundDetailsDto
    ): Int =
        client.post("/funds/add/with-details") {
            contentType(ContentType.Application.Json)
            setBody(AddFundWithDetailsRequest(fund = fund, details = details))
        }.unwrap<Int>()

    override suspend fun closeFund(
        fundId: Int,
        request: CloseFundRequest
    ): Boolean {

        return client.post(
            "funds/$fundId/close"
        ) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

}
