package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.data.mappers.toDto
import com.mynikatech.apnafund.data.model.*
import com.mynikatech.apnafund.net.dto.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LoansApiKtor(
    private val clientProvider: () -> io.ktor.client.HttpClient = { HttpClientProvider.client }
) : LoansApi {

    private val client get() = clientProvider()

    override suspend fun getAllLoans(): Flow<List<LoansDto>> = flow {
        val list: List<LoansDto> = client.get("/loans/get/all").unwrap<List<LoansDto>>()
        emit(list)
    }

    override fun getAllLoansforFund(fundId: Int): Flow<List<LoansDto>> = flow {
        val list: List<LoansDto> = client.get("/loans/get/fund/$fundId").unwrap<List<LoansDto>>()
        emit(list)
    }

    override suspend fun listByFund(fundId: Int): List<LoanDetailsWithMemberNamesDto> =
        client.get("/loans/get/with-names/fund/$fundId").unwrap<List<LoanDetailsWithMemberNamesDto>>()

    override suspend fun get(id: Int): LoanCmplDetailsDto? =
        client.get("/loans/get/complete/$id").unwrap<LoanCmplDetailsDto>()

    override suspend fun addLoan(dto: LoansDto): Int =
        client.post("/loans/add") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.unwrap<Int>()

    override suspend fun createLoan(loan: Loans): Long =
        client.post("/loans/create") {
            contentType(ContentType.Application.Json)
            setBody(loan)
        }.unwrap<Long>()

    override suspend fun updateLoan(id: Int, dto: LoansDto): Boolean =
        client.put("/loans/update/$id") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.unwrap<Boolean>()

    override suspend fun deleteLoan(id: Int): Boolean =
        client.delete("/loans/delete/$id").unwrap<Boolean>()

    // ---- EMIs ----

    override fun getAllLoanEmis(): Flow<List<LoanEmisDto>> = flow {
        val list: List<LoanEmisDto> = client.get("/loans/emis/get/all").unwrap<List<LoanEmisDto>>()
        emit(list)
    }

    override suspend fun addLoanEmi(dto: LoanEmisDto): Int =
        client.post("/loans/emis/add") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.unwrap<Int>()

    override fun getLoanEmisforLoan(loanId: Int): Flow<List<LoanEmisDto>> = flow {
        val list: List<LoanEmisDto> = client.get("/loans/emis/get/loan/$loanId").unwrap<List<LoanEmisDto>>()
        emit(list)
    }

    override suspend fun updateLoanEmis(loanEmis: LoanEmis): Boolean =
        client.put("/loans/emis/update/${loanEmis.loanEmiId}") {
            contentType(ContentType.Application.Json)
            setBody(loanEmis)
        }.unwrap<Boolean>()

    override suspend fun deleteLoanEmis(loanEmis: LoanEmis): Boolean =
        client.delete("/loans/emis/delete/${loanEmis.loanEmiId}").unwrap<Boolean>()

    override suspend fun getAllLoanEmisForFundForMonthYear(
        fundId: Int, month: String, year: String
    ): List<LoanEmiWithMemberNamesDto> =
        client.get("/loans/emis/get/with-names/fund/$fundId/$month/$year").unwrap<List<LoanEmiWithMemberNamesDto>>()

    override suspend fun saveOrUpdateAllLoanEmisAndFetch(
        loanEmis: List<LoanEmisDto>, fundId: Int, month: String, year: String
    ): List<LoanEmiWithMemberNamesDto> =
        client.post("/loans/emis/save-or-update/fetch") {
            val payload = SaveOrUpdateLoanEmisRequest(
                loanEmis = loanEmis,
                fundId = fundId,
                month = month,
                year = year
            )
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.unwrap<List<LoanEmiWithMemberNamesDto>>()

    override suspend fun getloanEmisForLoan(loanId: Int, emiMonth: String, emiYear: String): LoanEmisDto? =
        client.get("/loans/emis/get/one/loan/$loanId/$emiMonth/$emiYear").unwrap<LoanEmisDto>()

    override suspend fun getAllloanEmisForLoan(loanId: Int): List<LoanEmiWithMemberNamesDto>? =
        client.get("/loans/emis/get/with-names/loan/$loanId").unwrap<List<LoanEmiWithMemberNamesDto>>()

    // ---- Details ----

    override suspend fun getAllLoanDetails(loanId: Int): LoanDetailsDto? =
        client.get("/loans/details/get/$loanId").unwrap<LoanDetailsDto>()

    override suspend fun upsertDetails(dto: LoanDetailsDto): Int =
        client.post("/loans/details/upsert") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.unwrap<Int>()

    override suspend fun addLoanDetails(details: LoanDetails) {
        client.post("/loans/details/insert") {
            contentType(ContentType.Application.Json)
            setBody(details)
        }.body<Unit>()
    }

    override suspend fun updateLoanDetails(details: LoanDetails) {
        client.put("/loans/details/update") {
            contentType(ContentType.Application.Json)
            setBody(details)
        }.body<Unit>()
    }

    override suspend fun deleteLoanDetails(loanDetails: LoanDetails) {
        client.delete("/loans/details/delete/${loanDetails.loanDetailsId}").body<Unit>()
    }

    override suspend fun getAllLoanDetailsForFundForUser(
        fundId: Int, userId: Int
    ): List<LoanDetailsWithMemberNamesDto> =
        client.get("/loans/details/get/with-names/fund/$fundId/user/$userId")
            .unwrap<List<LoanDetailsWithMemberNamesDto>>()


    override suspend fun getLoanDetailsforLoan(loanId: Int): LoanCmplDetailsDto =
        client.get("/loans/details/get/$loanId").unwrap<LoanCmplDetailsDto>()

    override suspend fun isLoanforUserforFund(userId: Int, fundId: Int): Boolean =
        client.get("/loans/exists") {
            parameter("userId", userId); parameter("fundId", fundId)
        }.unwrap<Boolean>()

    override suspend fun insertLoanWithDetails(loan: Loans, laonDetails: LoanDetails) {
        client.post("/loans/insert/with-details") {
            contentType(ContentType.Application.Json)
            val payload = InsertWithLoanDetailsRequest(loan = loan.toDto(), details = laonDetails.toDto())
            setBody(payload)
        }.unwrap<Int>()
    }

    override suspend fun updateLoanWithDetails(loan: Loans, loanDetails: LoanDetails) {
        client.put("/loans/update/with-details") {
            contentType(ContentType.Application.Json)
            val payload = UpdateWithLoanDetailsRequest(loan = loan.toDto(), details = loanDetails.toDto())
            setBody(payload)
        }.body<Unit>()
    }

    override suspend fun getTotalLoanAmount(userId: Int, fundId: Int): Double =
        client.get("/loans/total/amount") {
            parameter("userId", userId); parameter("fundId", fundId)
        }.unwrap<Double>()

    override suspend fun getTotalPendingAmount(userId: Int, fundId: Int): Double =
        client.get("/loans/total/pending") {
            parameter("userId", userId); parameter("fundId", fundId)
        }.unwrap<Double>()

    override suspend fun getTotalCurrIntPaid(userId: Int, fundId: Int): Double =
        client.get("/loans/total/intpaid") {
            parameter("userId", userId); parameter("fundId", fundId)
        }.unwrap<Double>()

    override suspend fun getUserLoanDetails(userId: Int, fundId: Int): UserLoanDetailsDto =
        client.get("/loans/user-details") {
            parameter("userId", userId); parameter("fundId", fundId)
        }.unwrap<UserLoanDetailsDto>()
}
