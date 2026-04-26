package com.mynikatech.apnafund.server.loans

import com.mynikatech.apnafund.net.dto.InsertWithLoanDetailsRequest
import com.mynikatech.apnafund.net.dto.LoanDetailsDto
import com.mynikatech.apnafund.net.dto.LoanEmisDto
import com.mynikatech.apnafund.net.dto.LoansDto
import com.mynikatech.apnafund.net.dto.SaveOrUpdateLoanEmisRequest
import com.mynikatech.apnafund.net.dto.UpdateWithLoanDetailsRequest
import com.mynikatech.apnafund.server.api.respondError
import com.mynikatech.apnafund.server.api.respondOk
import com.mynikatech.apnafund.server.approval.ApprovalSql
import com.mynikatech.apnafund.server.common.messaging.dispatch.EventDispatchService
import com.mynikatech.apnafund.server.common.messaging.factories.UserNotificationFactory
import com.mynikatech.apnafund.server.funds.FundsSql
import com.mynikatech.apnafund.server.notifications.NotificationService
import com.mynikatech.apnafund.server.users.UsersSql
import com.mynikatech.apnafund.server.users.safeRoute
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import io.ktor.server.application.log


fun Route.loansRoutes(
    sql: LoansSql,
    eventDispatchService: EventDispatchService,
    users: UsersSql,
    fundSql: FundsSql,
    notificationService: NotificationService,
    approvalSql: ApprovalSql
) =
    route("/loans") {

        // ---- Loans (core) ----
        get("get/all") { call.respondOk(sql.getLoans()) }

        get("get/fund/{fundId}") {
            val fid = call.parameters["fundId"]?.toIntOrNull()
                ?: return@get call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "fundId required"
                )
            call.respondOk(sql.getLoansForFund(fid))
        }

        get("get/with-names/fund/{fundId}") {

            try {

                val fid = call.parameters["fundId"]?.toIntOrNull()
                    ?: return@get call.respondError(
                        HttpStatusCode.BadRequest,
                        "validation",
                        "fundId required"
                    )

                val loans = sql.getLoanDetailsWithNamesForFund(fid)

                call.respondOk(loans)

            } catch (e: Exception) {

                call.application.log.error("Error fetching loans for fundId=${call.parameters["fundId"]}", e)

                call.respondError(
                    HttpStatusCode.InternalServerError,
                    "server_error",
                    "Unable to fetch fund loans"
                )
            }
        }

        get("get/complete/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "id required"
                )
            val row = sql.getLoanComplete(id).firstOrNull()
            if (row != null) call.respondOk(row) else call.respondError(
                HttpStatusCode.NotFound,
                "not_found",
                "Loan not found"
            )
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
                ?: return@put call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "id required"
                )
            val dto = call.receive<LoansDto>()
            val ok = sql.updateLoan(id, dto)
            if (!ok) return@put call.respondError(
                HttpStatusCode.NotFound,
                "not_found",
                "Loan not found"
            )
            call.respondOk(Unit, HttpStatusCode.NoContent)
        }

        delete("delete/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "id required"
                )
            val ok = sql.deleteLoan(id)
            if (!ok) return@delete call.respondError(
                HttpStatusCode.NotFound,
                "not_found",
                "Loan not found"
            )
            call.respondOk(Unit, HttpStatusCode.NoContent)
        }

        post("insert/with-details") {

            try {

                val body = call.receive<InsertWithLoanDetailsRequest>()

                val borrowerId =
                    body.loan.borrowerId
                        ?: error("borrowerId must be present")

                val fundId =
                    body.loan.fundId
                        ?: error("fundId required")

                val requesterUserId = body.requestorId

                val requestorUser =
                    users.getUserById(requesterUserId)
                        ?: error("Requestor not found")

                val requestorName = requestorUser.fullName ?: "User"

                val fund =
                    fundSql.getFund(fundId).firstOrNull()
                        ?: return@post call.respondError(
                            HttpStatusCode.NotFound,
                            "NOT_FOUND",
                            "Fund not found"
                        )

                val moderatorId = fund.moderator

                val moderatorUser =
                    users.getUserById(moderatorId)
                        ?: error("Moderator user not found")

                val borrowerUser =
                    users.getUserById(borrowerId)
                        ?: error("Borrower user not found")

                val isModeratorApplying =
                    requesterUserId == moderatorId

                val workflowStatus =
                    if (isModeratorApplying)
                        "APPROVED"
                    else
                        "PENDING_APPROVAL"
                val status =
                    if (isModeratorApplying)
                        "ACTIVE"
                    else
                        "PENDING"

                val loanId = sql.insertLoanWithDetails(
                    body.loan.copy(
                        workflowStatus = workflowStatus,
                        status = status
                    ),
                    body.details
                )

                // ---------------- AUTO APPROVED ----------------
                if (isModeratorApplying) {

                    eventDispatchService.dispatchUser(
                        UserNotificationFactory.loanApproved(
                            userId = borrowerId.toString(),
                            email = borrowerUser.emailId,
                            phone = "91${borrowerUser.phoneNumber}",
                            userName = borrowerUser.fullName ?: "User"
                        )
                    )

                    notificationService.notifyLoanApproved(
                        loanId,
                        fundId,
                        fund.fundName,
                        borrowerUser,
                        requestorName
                    )
                }
                // ---------------- NEED APPROVAL ----------------
                else {

                    approvalSql.createLoanApproval(
                        loanId,
                        requesterUserId,
                        moderatorId
                    )

                    notificationService.notifyLoanRequested(
                        loanId,
                        fundId,
                        fund.fundName,
                        borrowerUser.fullName,
                        moderatorUser
                    )
                }

                call.respondOk(loanId, HttpStatusCode.Created)

            } catch (ex: IllegalStateException) {

                call.application.log.error(
                    "Loan creation failed (IllegalStateException)",
                    ex
                )

                call.respondError(
                    HttpStatusCode.Conflict,
                    "LOAN_CREATION_FAILED",
                    ex.message ?: "Invalid state"
                )

            } catch (ex: org.jdbi.v3.core.statement.UnableToExecuteStatementException) {

                call.application.log.error(
                    "Database error during loan creation",
                    ex
                )

                call.respondError(
                    HttpStatusCode.Conflict,
                    "DB_ERROR",
                    ex.cause?.message ?: "Database constraint failed"
                )

            } catch (ex: Exception) {

                call.application.log.error(
                    "Unexpected error inserting loan",
                    ex
                )

                call.respondError(
                    HttpStatusCode.InternalServerError,
                    "SERVER_ERROR",
                    "Unable to create loan"
                )
            }
        }

        put("update/with-details") {
            val body = call.receive<UpdateWithLoanDetailsRequest>()
            val ok = sql.updateLoanWithDetails(body.loan, body.details)
            if (!ok) return@put call.respondError(
                HttpStatusCode.NotFound,
                "not_found",
                "Loan not found"
            )
            call.respondOk(Unit, HttpStatusCode.NoContent)
        }

        get("exists") {
            val uid = call.parameters["userId"]?.toIntOrNull()
                ?: return@get call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "userId required"
                )
            val fid = call.parameters["fundId"]?.toIntOrNull()
                ?: return@get call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "fundId required"
                )
            call.respondOk(mapOf("exists" to sql.isLoanForUserForFund(uid, fid)))
        }

        get("total/amount") {
            val uid = call.parameters["userId"]?.toIntOrNull()
                ?: return@get call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "userId required"
                )
            val fid = call.parameters["fundId"]?.toIntOrNull()
                ?: return@get call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "fundId required"
                )
            call.respondOk(mapOf("total" to (sql.getTotalLoanAmount(uid, fid) ?: 0.0)))
        }

        get("total/pending") {
            val uid = call.parameters["userId"]?.toIntOrNull()
                ?: return@get call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "userId required"
                )
            val fid = call.parameters["fundId"]?.toIntOrNull()
                ?: return@get call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "fundId required"
                )
            call.respondOk(mapOf("total" to (sql.getTotalPendingAmount(uid, fid) ?: 0.0)))
        }

        get("total/intpaid") {
            val uid = call.parameters["userId"]?.toIntOrNull()
                ?: return@get call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "userId required"
                )
            val fid = call.parameters["fundId"]?.toIntOrNull()
                ?: return@get call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "fundId required"
                )
            call.respondOk(mapOf("total" to (sql.getTotalCurrIntPaid(uid, fid) ?: 0.0)))
        }

        get("user-details") {
            val uid = call.parameters["userId"]?.toIntOrNull()
                ?: return@get call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "userId required"
                )
            val fid = call.parameters["fundId"]?.toIntOrNull()
                ?: return@get call.respondError(
                    HttpStatusCode.BadRequest,
                    "validation",
                    "fundId required"
                )
            val row = sql.getUserLoanDetails(uid, fid).firstOrNull()
            if (row != null) call.respondOk(row) else call.respondOk(HttpStatusCode.NoContent)
        }

        // ---- EMIs ----
        route("emis") {

            get("get/all") { call.respondOk(sql.getAllLoanEmis()) }

            get("get/loan/{loanId}") {
                val lid = call.parameters["loanId"]?.toIntOrNull()
                    ?: return@get call.respondError(
                        HttpStatusCode.BadRequest,
                        "validation",
                        "loanId required"
                    )
                call.respondOk(sql.getLoanEmisForLoan(lid))
            }

            get("get/one/loan/{loanId}/{month}/{year}") {
                val lid = call.parameters["loanId"]?.toIntOrNull()
                    ?: return@get call.respondError(
                        HttpStatusCode.BadRequest,
                        "validation",
                        "loanId required"
                    )
                val month = call.parameters["month"]!!
                val year = call.parameters["year"]!!
                val row = sql.getLoanEmiForLoanMonthYear(lid, month, year).firstOrNull()
                if (row != null) call.respondOk(row) else call.respondError(
                    HttpStatusCode.NotFound,
                    "not_found",
                    "EMI not found"
                )
            }

            get("get/with-names/loan/{loanId}") {
                val lid = call.parameters["loanId"]?.toIntOrNull()
                    ?: return@get call.respondError(
                        HttpStatusCode.BadRequest,
                        "validation",
                        "loanId required"
                    )
                call.respondOk(sql.getAllLoanEmisWithNamesForLoan(lid))
            }

            get("get/with-names/fund/{fundId}/{month}/{year}") {
                val fid = call.parameters["fundId"]?.toIntOrNull()
                    ?: return@get call.respondError(
                        HttpStatusCode.BadRequest,
                        "validation",
                        "fundId required"
                    )
                val month = call.parameters["month"]!!
                val year = call.parameters["year"]!!
                call.respondOk(sql.getAllLoanEmisWithNamesForFundMonthYear(fid, month, year))
            }

            post("add") {
                val dto = call.receive<LoanEmisDto>()
                val id = sql.addLoanEmi(dto)
                call.respondOk(id, HttpStatusCode.Created)
            }

            put("update/{loanEmiId}") {
                val id = call.parameters["loanEmiId"]?.toIntOrNull()
                    ?: return@put call.respondError(
                        HttpStatusCode.BadRequest,
                        "validation",
                        "loanEmiId required"
                    )
                val body = call.receive<LoanEmisDto>()
                val ok = sql.updateLoanEmi(id, body)
                if (!ok) return@put call.respondError(
                    HttpStatusCode.NotFound,
                    "not_found",
                    "EMI not found"
                )
                call.respondOk(Unit, HttpStatusCode.NoContent)
            }

            delete("delete/{loanEmiId}") {
                val id = call.parameters["loanEmiId"]?.toIntOrNull()
                    ?: return@delete call.respondError(
                        HttpStatusCode.BadRequest,
                        "validation",
                        "loanEmiId required"
                    )
                val ok = sql.deleteLoanEmi(id)
                if (!ok) return@delete call.respondError(
                    HttpStatusCode.NotFound,
                    "not_found",
                    "EMI not found"
                )
                call.respondOk(Unit, HttpStatusCode.NoContent)
            }

            // Batch save-or-update then fetch (DB does the heavy logic)
            post("save-or-update/fetch") {
                val req = call.receive<SaveOrUpdateLoanEmisRequest>()

                val json = Json { explicitNulls = false }
                val loanEmis: String =
                    json.encodeToString(ListSerializer(LoanEmisDto.serializer()), req.loanEmis)
                val prevStatusMap = mutableMapOf<Int, LoansDto?>()
                req.loanEmis
                    .map { it.loanId }
                    .distinct()
                    .forEach { loanId ->
                        prevStatusMap[loanId] = sql.getLoanById(loanId)
                    }
                val rows =
                    sql.saveOrUpdateAllLoanEmisAndFetch(req.fundId, req.month, req.year, loanEmis)

                val newlyClosedLoans = rows
                    .distinctBy { it.loanId }
                    .filter { row ->
                        val prev = prevStatusMap[row.loanId]
                        prev != null &&
                                prev.status != "CLOSED" &&
                                row.status == "CLOSED"
                    }
                newlyClosedLoans.forEach { loan ->
                    val loanId = loan.loanId ?: return@forEach
                    val borrowerUser = users.getUserById(loan.borrowerId)
                    val fund = fundSql.getFund(loan.fundId).firstOrNull()
                    val fundName = fund?.fundName ?: "Fund"
                    val moderatorUser = fund?.moderator
                        ?.let { users.getUserById(it) }

                    val closedByName = moderatorUser?.fullName ?: "System"

                    notificationService.notifyLoanClosed(
                        loanId = loanId,
                        fundId = loan.fundId,
                        fundName = fundName,
                        borrower = borrowerUser,
                        closureType = loan.closureType ?: "UNKNOWN",
                        closedByName = closedByName
                    )
                }
                call.respondOk(rows)
            }
        }

        // ---- Details ----
        route("details") {

            get("get/{loanId}") {
                val lid = call.parameters["loanId"]?.toIntOrNull()
                    ?: return@get call.respondError(
                        HttpStatusCode.BadRequest,
                        "validation",
                        "loanId required"
                    )
                val d = sql.getLoanDetails(lid).firstOrNull()
                if (d != null) call.respondOk(d) else call.respondError(
                    HttpStatusCode.NotFound,
                    "not_found",
                    "Details not found"
                )
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
                    ?: return@delete call.respondError(
                        HttpStatusCode.BadRequest,
                        "validation",
                        "loanDetailsId required"
                    )
                val ok = sql.deleteLoanDetails(id)
                if (!ok) return@delete call.respondError(
                    HttpStatusCode.NotFound,
                    "not_found",
                    "Details not found"
                )
                call.respondOk(Unit, HttpStatusCode.NoContent)
            }

            get("get/with-names/fund/{fundId}/user/{userId}") {
                val fundId = call.parameters["fundId"]?.toIntOrNull()
                val userId = call.parameters["userId"]?.toIntOrNull()
                if (fundId == null || userId == null) {
                    return@get call.respondError(
                        HttpStatusCode.BadRequest,
                        "validation",
                        "fundId & userId required"
                    )
                }
                call.safeRoute(
                    logMessage = "GET get/with-names/fund/{fundId}/user/{userId}: ${fundId}, ${userId}",
                    clientMessage = "Failed to fetch user"
                ) {
                    val rows = sql.getLoanDetailsWithNamesForFundUser(fundId, userId)
                    rows
                }
            }
        }
    }

inline suspend fun <reified T> ApplicationCall.safeRoute(
    logMessage: String,
    clientMessage: String,
    block: suspend () -> T
) {
    try {
        respondOk(block())
    } catch (e: Exception) {

        application.log.error(logMessage, e)

        respondError(
            HttpStatusCode.InternalServerError,
            "internal",
            clientMessage
        )
    }
}
