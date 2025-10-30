package com.mynikatech.apnafund.server.loans

import com.mynikatech.apnafund.net.dto.*
import com.mynikatech.apnafund.server.api.respondError
import com.mynikatech.apnafund.server.api.respondOk
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json



fun Route.loansRoutes(sql: LoansSql) = route("/loans") {

    // ---- Loans (core) ----
    get("get/all") { call.respondOk(sql.getLoans()) }

    get("get/fund/{fundId}") {
        val fid = call.parameters["fundId"]?.toIntOrNull()
            ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "fundId required")
        call.respondOk(sql.getLoansForFund(fid))
    }

    get("get/with-names/fund/{fundId}") {
        val fid = call.parameters["fundId"]?.toIntOrNull()
            ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "fundId required")
        call.respondOk(sql.getLoanDetailsWithNamesForFund(fid))
    }

    get("get/complete/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "id required")
        val row = sql.getLoanComplete(id).firstOrNull()
        if (row != null) call.respondOk(row) else call.respondError(HttpStatusCode.NotFound, "not_found", "Loan not found")
    }

    post("add") {
        val dto = call.receive<LoansDto>()
        val id = sql.addLoan(dto)
        call.respondOk(id, HttpStatusCode.Created)
    }

    post("create") {
        val loan = call.receive<LoansDto>()
        val id = sql.addLoan(loan).toLong()
        call.respondOk(id, HttpStatusCode.Created)
    }

    put("update/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@put call.respondError(HttpStatusCode.BadRequest, "validation", "id required")
        val dto = call.receive<LoansDto>()
        val ok = sql.updateLoan(id, dto)
        if (!ok) return@put call.respondError(HttpStatusCode.NotFound, "not_found", "Loan not found")
        call.respondOk(Unit, HttpStatusCode.NoContent)
    }

    delete("delete/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@delete call.respondError(HttpStatusCode.BadRequest, "validation", "id required")
        val ok = sql.deleteLoan(id)
        if (!ok) return@delete call.respondError(HttpStatusCode.NotFound, "not_found", "Loan not found")
        call.respondOk(Unit, HttpStatusCode.NoContent)
    }

    post("insert/with-details") {
        val body = call.receive<InsertWithLoanDetailsRequest>()
        val id = sql.insertLoanWithDetails(body.loan, body.details)
        call.respondOk(id, HttpStatusCode.Created)
    }

    put("update/with-details") {
        val body = call.receive<UpdateWithLoanDetailsRequest>()
        val ok = sql.updateLoanWithDetails(body.loan, body.details)
        if (!ok) return@put call.respondError(HttpStatusCode.NotFound, "not_found", "Loan not found")
        call.respondOk(Unit, HttpStatusCode.NoContent)
    }

    get("exists") {
        val uid = call.parameters["userId"]?.toIntOrNull()
            ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "userId required")
        val fid = call.parameters["fundId"]?.toIntOrNull()
            ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "fundId required")
        call.respondOk(mapOf("exists" to sql.isLoanForUserForFund(uid, fid)))
    }

    get("total/amount") {
        val uid = call.parameters["userId"]?.toIntOrNull()
            ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "userId required")
        val fid = call.parameters["fundId"]?.toIntOrNull()
            ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "fundId required")
        call.respondOk(mapOf("total" to (sql.getTotalLoanAmount(uid, fid) ?: 0.0)))
    }

    get("total/pending") {
        val uid = call.parameters["userId"]?.toIntOrNull()
            ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "userId required")
        val fid = call.parameters["fundId"]?.toIntOrNull()
            ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "fundId required")
        call.respondOk(mapOf("total" to (sql.getTotalPendingAmount(uid, fid) ?: 0.0)))
    }

    get("total/intpaid") {
        val uid = call.parameters["userId"]?.toIntOrNull()
            ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "userId required")
        val fid = call.parameters["fundId"]?.toIntOrNull()
            ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "fundId required")
        call.respondOk(mapOf("total" to (sql.getTotalCurrIntPaid(uid, fid) ?: 0.0)))
    }

    get("user-details") {
        val uid = call.parameters["userId"]?.toIntOrNull()
            ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "userId required")
        val fid = call.parameters["fundId"]?.toIntOrNull()
            ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "fundId required")
        val row = sql.getUserLoanDetails(uid, fid).firstOrNull()
        if (row != null) call.respondOk(row) else call.respondOk(HttpStatusCode.NoContent)
    }

    // ---- EMIs ----
    route("emis") {

        get("get/all") { call.respondOk(sql.getAllLoanEmis()) }

        get("get/loan/{loanId}") {
            val lid = call.parameters["loanId"]?.toIntOrNull()
                ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "loanId required")
            call.respondOk(sql.getLoanEmisForLoan(lid))
        }

        get("get/one/loan/{loanId}/{month}/{year}") {
            val lid = call.parameters["loanId"]?.toIntOrNull()
                ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "loanId required")
            val month = call.parameters["month"]!!
            val year  = call.parameters["year"]!!
            val row = sql.getLoanEmiForLoanMonthYear(lid, month, year).firstOrNull()
            if (row != null) call.respondOk(row) else call.respondError(HttpStatusCode.NotFound, "not_found", "EMI not found")
        }

        get("get/with-names/loan/{loanId}") {
            val lid = call.parameters["loanId"]?.toIntOrNull()
                ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "loanId required")
            call.respondOk(sql.getAllLoanEmisWithNamesForLoan(lid))
        }

        get("get/with-names/fund/{fundId}/{month}/{year}") {
            val fid = call.parameters["fundId"]?.toIntOrNull()
                ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "fundId required")
            val month = call.parameters["month"]!!
            val year  = call.parameters["year"]!!
            call.respondOk(sql.getAllLoanEmisWithNamesForFundMonthYear(fid, month, year))
        }

        post("add") {
            val dto = call.receive<LoanEmisDto>()
            val id = sql.addLoanEmi(dto)
            call.respondOk(id, HttpStatusCode.Created)
        }

        put("update/{loanEmiId}") {
            val id = call.parameters["loanEmiId"]?.toIntOrNull()
                ?: return@put call.respondError(HttpStatusCode.BadRequest, "validation", "loanEmiId required")
            val body = call.receive<LoanEmisDto>()
            val ok = sql.updateLoanEmi(id, body)
            if (!ok) return@put call.respondError(HttpStatusCode.NotFound, "not_found", "EMI not found")
            call.respondOk(Unit, HttpStatusCode.NoContent)
        }

        delete("delete/{loanEmiId}") {
            val id = call.parameters["loanEmiId"]?.toIntOrNull()
                ?: return@delete call.respondError(HttpStatusCode.BadRequest, "validation", "loanEmiId required")
            val ok = sql.deleteLoanEmi(id)
            if (!ok) return@delete call.respondError(HttpStatusCode.NotFound, "not_found", "EMI not found")
            call.respondOk(Unit, HttpStatusCode.NoContent)
        }

        // Batch save-or-update then fetch (DB does the heavy logic)
        post("save-or-update/fetch") {
            val req = call.receive<SaveOrUpdateLoanEmisRequest>()

            val json = Json { explicitNulls = false }
            val loanEmis: String = json.encodeToString(ListSerializer(LoanEmisDto.serializer()), req.loanEmis)
            val rows = sql.saveOrUpdateAllLoanEmisAndFetch(req.fundId, req.month, req.year, loanEmis)
            call.respondOk(rows)
        }
    }

    // ---- Details ----
    route("details") {

        get("get/{loanId}") {
            val lid = call.parameters["loanId"]?.toIntOrNull()
                ?: return@get call.respondError(HttpStatusCode.BadRequest, "validation", "loanId required")
            val d = sql.getLoanDetails(lid).firstOrNull()
            if (d != null) call.respondOk(d) else call.respondError(HttpStatusCode.NotFound, "not_found", "Details not found")
        }

        post("upsert") {
            val dto = call.receive<LoanDetailsDto>()
            val id = sql.upsertLoanDetails(dto)
            call.respondOk(id)
        }

        post("insert") {
            val dto = call.receive<LoanDetailsDto>()
            sql.insertLoanDetails(dto)
            call.respondOk(Unit, HttpStatusCode.NoContent)
        }

        put("update") {
            val dto = call.receive<LoanDetailsDto>()
            sql.updateLoanDetails(dto)
            call.respondOk(Unit, HttpStatusCode.NoContent)
        }

        delete("delete/{loanDetailsId}") {
            val id = call.parameters["loanDetailsId"]?.toIntOrNull()
                ?: return@delete call.respondError(HttpStatusCode.BadRequest, "validation", "loanDetailsId required")
            val ok = sql.deleteLoanDetails(id)
            if (!ok) return@delete call.respondError(HttpStatusCode.NotFound, "not_found", "Details not found")
            call.respondOk(Unit, HttpStatusCode.NoContent)
        }

        get("get/with-names/fund/{fundId}/user/{userId}") {
            val fundId = call.parameters["fundId"]?.toIntOrNull()
            val userId = call.parameters["userId"]?.toIntOrNull()
            if (fundId == null || userId == null) {
                return@get call.respondError(HttpStatusCode.BadRequest, "validation", "fundId & userId required")
            }
            val rows = sql.getLoanDetailsWithNamesForFundUser(fundId, userId)
            call.respondOk(rows)
        }
    }
}
